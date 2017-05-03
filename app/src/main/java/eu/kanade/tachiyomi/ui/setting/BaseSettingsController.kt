package eu.kanade.tachiyomi.ui.setting

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceController
import android.support.v7.preference.PreferenceScreen
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import rx.Observable
import rx.Subscription
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

abstract class BaseSettingsController : PreferenceController() {

    val preferences: PreferencesHelper = Injekt.get()

    val keys = preferences.keys

    var untilDestroySubscriptions = CompositeSubscription()
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        if (untilDestroySubscriptions.isUnsubscribed) {
            untilDestroySubscriptions = CompositeSubscription()
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        untilDestroySubscriptions.unsubscribe()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val screen = preferenceManager.createPreferenceScreen(getThemedContext())
        preferenceScreen = screen
        setupPreferenceScreen(screen)
    }

    abstract fun setupPreferenceScreen(screen: PreferenceScreen): Any?

    private fun getThemedContext(): Context {
        val tv = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        return ContextThemeWrapper(activity, tv.resourceId)
    }

    open fun getTitle(): String? {
        return preferenceScreen?.title?.toString()
    }

    override fun onAttach(view: View) {
        (activity as? AppCompatActivity)?.supportActionBar?.title = getTitle()
        super.onAttach(view)
    }

    fun <T> Observable<T>.subscribeUntilDestroy(): Subscription {
        return subscribe().also { untilDestroySubscriptions.add(it) }
    }

    fun <T> Observable<T>.subscribeUntilDestroy(onNext: (T) -> Unit): Subscription {
        return subscribe(onNext).also { untilDestroySubscriptions.add(it) }
    }
}