package eu.kanade.tachiyomi.ui.setting

import android.support.v7.preference.PreferenceScreen
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.updater.GithubUpdateChecker
import eu.kanade.tachiyomi.data.updater.GithubUpdateResult
import eu.kanade.tachiyomi.data.updater.UpdateCheckerJob
import eu.kanade.tachiyomi.data.updater.UpdateDownloaderService
import eu.kanade.tachiyomi.util.toast
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class SettingsAboutController : BaseSettingsController() {

    /**
     * Checks for new releases
     */
    private val updateChecker by lazy { GithubUpdateChecker() }

    /**
     * The subscribtion service of the obtained release object
     */
    private var releaseSubscription: Subscription? = null

    private val isUpdaterEnabled = !BuildConfig.DEBUG && BuildConfig.INCLUDE_UPDATER

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_about

        switchPreference {
            key = "acra.enable"
            titleRes = R.string.pref_enable_acra
            summaryRes = R.string.pref_acra_summary
            defaultValue = true
        }
        switchPreference {
            key = keys.automaticUpdates
            titleRes = R.string.pref_enable_automatic_updates
            summaryRes = R.string.pref_enable_automatic_updates_summary
            defaultValue = false

            if (isUpdaterEnabled) {
                onChange { newValue ->
                    val checked = newValue as Boolean
                    if (checked) {
                        UpdateCheckerJob.setupTask()
                    } else {
                        UpdateCheckerJob.cancelTask()
                    }
                    true
                }
            } else {
                isVisible = false
            }
        }
        preference {
            titleRes = R.string.version
            summary = if (BuildConfig.DEBUG)
                "r" + BuildConfig.COMMIT_COUNT
            else
                BuildConfig.VERSION_NAME

            if (isUpdaterEnabled) {
                onClick { checkVersion() }
            }
        }
        preference {
            titleRes = R.string.build_time
            summary = getFormattedBuildTime()
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        releaseSubscription?.unsubscribe()
        releaseSubscription = null
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    private fun checkVersion() {
        if (activity == null) return

        activity?.toast(R.string.update_check_look_for_updates)
        releaseSubscription?.unsubscribe()
        releaseSubscription = updateChecker.checkForUpdate()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    val activity = activity ?: return@subscribe
                    when (result) {
                        is GithubUpdateResult.NewUpdate -> {
                            val body = result.release.changeLog
                            val url = result.release.downloadLink

                            // Create confirmation window
                            MaterialDialog.Builder(activity)
                                    .title(R.string.update_check_title)
                                    .content(body)
                                    .positiveText(R.string.update_check_confirm)
                                    .negativeText(R.string.update_check_ignore)
                                    .onPositive { _, _ ->
                                        val appContext = applicationContext
                                        if (appContext != null) {
                                            // Start download
                                            UpdateDownloaderService.downloadUpdate(appContext, url)
                                        }
                                    }
                                    .show()
                        }
                        is GithubUpdateResult.NoNewUpdate -> {
                            activity.toast(R.string.update_check_no_new_updates)
                        }
                    }
                }, { error ->
                    Timber.e(error)
                })
    }

    private fun getFormattedBuildTime(): String {
        try {
            val inputDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US)
            inputDf.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputDf.parse(BuildConfig.BUILD_TIME)

            val outputDf = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
            outputDf.timeZone = TimeZone.getDefault()

            return outputDf.format(date)
        } catch (e: ParseException) {
            return BuildConfig.BUILD_TIME
        }
    }
}
