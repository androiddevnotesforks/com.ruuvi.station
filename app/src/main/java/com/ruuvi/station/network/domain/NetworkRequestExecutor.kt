package com.ruuvi.station.network.domain

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.database.domain.NetworkRequestRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.model.NetworkRequestStatus
import com.ruuvi.station.database.model.NetworkRequestType
import com.ruuvi.station.database.tables.NetworkRequest
import com.ruuvi.station.network.data.request.*
import com.ruuvi.station.network.data.requestWrappers.UploadImageRequestWrapper
import com.ruuvi.station.network.data.response.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import timber.log.Timber

class NetworkRequestExecutor (
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository,
    private val networkRequestRepository: NetworkRequestRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val jobManager: NetworkJobManager
){
    private fun getToken() = tokenRepository.getTokenInfo()

    fun registerRequest(networkRequest: NetworkRequest, executeNow: Boolean = true) {
        Timber.d("registerRequest $networkRequest $executeNow")
        CoroutineScope(Dispatchers.IO).launch {
            disableSimilarRequest (networkRequest)
            networkRequestRepository.saveRequest(networkRequest)
            Timber.d("request saved $networkRequest")
            if (executeNow) {
                Timber.d("execute NOW $networkRequest")
                delay(1000)
                val request = networkRequestRepository.getById(networkRequest.id)
                if (request != null && request.status == NetworkRequestStatus.READY) {
                    execute(request)
                }
            }
        }
    }

    fun registerRequestWithStatus(networkRequest: NetworkRequest): Flow<OperationStatus> = channelFlow {

        Timber.d("registerRequest $networkRequest true")
        CoroutineScope(Dispatchers.IO).launch {
            send(OperationStatus.InProgress)
            disableSimilarRequest (networkRequest)
            networkRequestRepository.saveRequest(networkRequest)
            Timber.d("request saved $networkRequest")
            Timber.d("execute NOW $networkRequest")
            val result = execute(networkRequest)
            if (result) {
                send(OperationStatus.Success)
            } else {
                send(OperationStatus.Fail(UiText.EmptyString))
            }
        }
        awaitClose()
    }

    fun gotAnyImagesInSync(sensorId: String): Boolean {
        return networkRequestRepository.getActiveRequestsForKeyType(sensorId, NetworkRequestType.UPLOAD_IMAGE).any()
    }

    private fun disableSimilarRequest(networkRequest: NetworkRequest) {
        val similarRequests = networkRequestRepository.getSimilar(networkRequest)
        for (request in similarRequests) {
            jobManager.cancelJob(request.id)
        }
        networkRequestRepository.disableSimilar(networkRequest)
    }

    suspend fun executeScheduledRequests() {
        Timber.d("executeScheduledRequests")
        jobManager.jobsToLog()
        val requests = networkRequestRepository.getScheduledRequests()
        Timber.d("executeScheduledRequests activeRequests = ${requests.size}")
        for (networkRequest in requests) {
            execute(networkRequest)
        }
    }

    fun anySettingsRequests(): Boolean {
        val requests = networkRequestRepository.getScheduledRequests()
        return requests.any{ it.type == NetworkRequestType.SETTINGS }
    }

    private suspend fun execute(networkRequest: NetworkRequest): Boolean {
        if (!startExecuting(networkRequest)) {
            return false
        }

        val token = getToken()?.token
        val request = getRequest(networkRequest)
        var result = false

        if (request != null) {
            token?.let {
                try {
                    val response = runSpecificAction(token, networkRequest, request)
                    Timber.d("Execute response: $response")
                    if (response?.isSuccess() == true) {
                        disableRequest(networkRequest, NetworkRequestStatus.SUCCESS)
                        result = true
                    } else {
                        if (response?.code == ER_CONFLICT) {
                            disableRequest(networkRequest, NetworkRequestStatus.CONFLICT)
                        } else {
                            registerFailedAttempt(networkRequest)
                        }
                    }
                } catch (e: Exception) {
                    Timber.d("Exception catched: ${e.message}")
                    registerFailedAttempt(networkRequest)
                }
            }
        } else {
            disableRequest(networkRequest, NetworkRequestStatus.PARSE_FAIL)
        }
        return result
    }

    private fun startExecuting(networkRequest: NetworkRequest): Boolean {
        if (jobManager.isJobRunning(networkRequest.id)) {
            Timber.d("Job ${networkRequest.id} is already running")
            return false
        }

        val result = networkRequestRepository.startExecuting(networkRequest)
        Timber.d("startExecuting $result $networkRequest")
        return result
    }

    private fun getRequest(networkRequest: NetworkRequest): Any? {
        with(networkRequest){
            return when (type) {
                NetworkRequestType.UNCLAIM -> parseJson<UnclaimSensorRequest>(requestData)
                NetworkRequestType.UPDATE_SENSOR -> parseJson<UpdateSensorRequest>(requestData)
                NetworkRequestType.UPLOAD_IMAGE -> parseJson<UploadImageRequestWrapper>(requestData)
                NetworkRequestType.SETTINGS -> parseJson<UpdateUserSettingRequest>(requestData)
                NetworkRequestType.UNSHARE -> parseJson<UnshareSensorRequest>(requestData)
                NetworkRequestType.RESET_IMAGE -> parseJson<UploadImageRequest>(requestData)
                NetworkRequestType.SET_ALERT -> parseJson<SetAlertRequest>(requestData)
            }
        }
    }

    private suspend fun runSpecificAction(token:String, networkRequest: NetworkRequest, request: Any?): RuuviNetworkResponse<*>? {
        Timber.d("runSpecificAction $networkRequest")
        var response: RuuviNetworkResponse<*>? = null

        val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
            Timber.d("runSpecificAction exception: ${throwable.message} ${throwable.stackTrace}")
        }

        val job = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            response =  when (networkRequest.type) {
                NetworkRequestType.UNCLAIM -> unclaimSensor(token, request as UnclaimSensorRequest)
                NetworkRequestType.UPDATE_SENSOR -> updateSensor(
                    token,
                    request as UpdateSensorRequest
                )
                NetworkRequestType.UPLOAD_IMAGE -> uploadImage(
                    token,
                    request as UploadImageRequestWrapper
                )
                NetworkRequestType.SETTINGS -> updateUserSettings(
                    token,
                    request as UpdateUserSettingRequest
                )
                NetworkRequestType.UNSHARE -> unshareSensor(token, request as UnshareSensorRequest)
                NetworkRequestType.RESET_IMAGE -> resetImage(token, request as UploadImageRequest)
                NetworkRequestType.SET_ALERT -> setAlert(token, request as SetAlertRequest)
            }
        }
        jobManager.registerJob(networkRequest.id, job)
        job.join()
        return response
    }

    private suspend fun setAlert(token: String, request: SetAlertRequest): SetAlertResponse? {
        Timber.d("setAlert $request")
        return networkRepository.setAlert(request, token)
    }

    private suspend fun unclaimSensor(token: String, request: UnclaimSensorRequest): ClaimSensorResponse? {
        Timber.d("unclaimSensor $request")
        return networkRepository.unclaimSensor(request, token)
    }

    private suspend fun updateSensor(token: String, request: UpdateSensorRequest): UpdateSensorResponse? {
        Timber.d("updateSensor $request")
        return networkRepository.updateSensor(request, token)
    }

    private suspend fun unshareSensor(token: String, request: UnshareSensorRequest): ShareSensorResponse? {
        Timber.d("unshareSensor $request")
        return networkRepository.unshareSensor(request, token)
    }

    private suspend fun uploadImage(token: String, request: UploadImageRequestWrapper): UploadImageResponse? {
        Timber.d("uploadImage")
        val response = networkRepository.uploadImage(request.filename, request.request, token)
        delay(1)
        if (response?.isSuccess() == true && !response.data?.guid.isNullOrEmpty()) {
            Timber.d("uploadImage-updateNetworkBackground")
            sensorSettingsRepository.updateNetworkBackground(request.request.sensor, response.data?.guid)
        }
        return response
    }

    private suspend fun resetImage(token: String, request: UploadImageRequest): UploadImageResponse? {
        Timber.d("resetImage $request")
        return networkRepository.resetImage(request, token)
    }

    private suspend fun updateUserSettings(token: String, request: UpdateUserSettingRequest): UpdateUserSettingResponse? {
        Timber.d("updateUserSettings $request")
        return networkRepository.updateUserSettings(request, token)
    }

    private inline fun <reified T>parseJson(jsonString: String): T? {
        return try {
            Gson().fromJson(jsonString, T::class.java)
        } catch (e: Exception) {
            Timber.e(e)
            with (FirebaseCrashlytics.getInstance()){
                log("parseJson = $jsonString")
                recordException(e)
            }
            null
        }
    }

    private fun disableRequest(networkRequest: NetworkRequest, status: NetworkRequestStatus) {
        Timber.d("disableRequest $networkRequest")
        networkRequestRepository.disableRequest(networkRequest, status)
    }

    private fun registerFailedAttempt(networkRequest: NetworkRequest) {
        Timber.d("registerFailedAttempt $networkRequest")
        networkRequestRepository.registerFailedAttempt(networkRequest)
    }

    class NetworkJobManager() {
        private val jobs: MutableMap<Int, Job> = mutableMapOf()

        fun jobsToLog() {
            val log = StringBuilder()
            log.appendLine("NETWORK JOBS")
            for (job in jobs.entries) {
                val isActive = job.value.isActive
                log.appendLine("job ${job.key} $isActive")
            }
            Timber.d(log.toString())
        }

        fun registerJob(id: Int, job: Job) {
            Timber.d("registerJob $id")
            if (!jobs.containsKey(id)) {
                jobs[id] = job
            }
        }

        fun cancelJob(id: Int) {
            Timber.d("cancelJob $id")
            if (jobs.containsKey(id)) {
                val job = jobs[id]
                val isActive = job?.isActive
                Timber.d("Canceling job $id isActive $isActive")
                job?.cancel()
            } else {
                Timber.d("job $id not found")
            }
        }

        fun isJobRunning(id: Int): Boolean {
            if (jobs.containsKey(id)) {
                return jobs[id]?.isActive == true
            }
            return false
        }
    }

    companion object {
        private val ER_CONFLICT = "ER_CONFLICT"
    }
}