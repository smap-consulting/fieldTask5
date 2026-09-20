package org.odk.collect.android.application.initialization

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.startup.AppInitializer
import net.danlew.android.joda.JodaTimeInitializer
import org.odk.collect.analytics.Analytics
import org.odk.collect.android.BuildConfig
import org.odk.collect.android.application.Collect
import org.odk.collect.android.application.initialization.upgrade.UpgradeInitializer
import org.odk.collect.android.entities.EntitiesRepositoryProvider
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.androidshared.ui.ToastUtils
import org.odk.collect.async.Scheduler
import org.odk.collect.forms.FormsRepository
import org.odk.collect.forms.instances.InstancesRepository
import org.odk.collect.metadata.PropertyManager
import org.odk.collect.projects.ProjectDependencyFactory
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.SettingsProvider
import timber.log.Timber
import java.util.Locale

class ApplicationInitializer(
    private val context: Application,
    private val propertyManager: PropertyManager,
    private val analytics: Analytics,
    private val upgradeInitializer: UpgradeInitializer,
    private val analyticsInitializer: AnalyticsInitializer,
    private val mapsInitializer: MapsInitializer,
    private val projectsRepository: ProjectsRepository,
    private val settingsProvider: SettingsProvider,
    private val entitiesRepositoryProvider: EntitiesRepositoryProvider,
    private val projectsDataService: ProjectsDataService,
    private val scheduler: Scheduler,
    private val instancesRepositoryProvider: ProjectDependencyFactory<InstancesRepository>,
    private val formsRepositoryProvider: ProjectDependencyFactory<FormsRepository>
) {
    fun initialize() {
        initializeLocale()
        runInitializers()
        initializeFrameworks()
    }

    private fun runInitializers() {
        upgradeInitializer.initialize()
        analyticsInitializer.initialize()
        UserPropertiesInitializer(
            analytics,
            projectsRepository,
            settingsProvider,
            context,
            scheduler,
            instancesRepositoryProvider,
            formsRepositoryProvider
        ).initialize()
        mapsInitializer.initialize()
        JavaRosaInitializer(propertyManager, projectsDataService, entitiesRepositoryProvider).initialize()
        CoilInitializer().initialize()
    }

    private fun initializeFrameworks() {
        ToastUtils.setApplication(context)
        initializeLogging()
        AppInitializer.getInstance(context).initializeComponent(JodaTimeInitializer::class.java)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    private fun initializeLocale() {
        Collect.defaultSysLanguage = Locale.getDefault().language
    }

    private fun initializeLogging() {
        // smap - was gated on BUILD_TYPE == "odkCollectRelease", which is upstream ODK's
        // release type.  Smap ships assembleStandardRelease, so every shipped build took
        // the debug branch and left debug logging on for users, which is what planting the
        // right tree here stops.  Only the debug build type sets debuggable, so
        // BuildConfig.DEBUG covers release, odkCollectRelease, selfSignedRelease and
        // anything added later.
        //
        // Note CrashReportingTree does not currently report anything.  It forwards to the
        // injected Analytics, and FieldTask provides NoopAnalytics deliberately because it
        // collects no usage data.  Crashes are still reported, by Crashlytics' own uncaught
        // exception handler, which does not go through Timber at all.  So handled errors
        // logged with Timber.e stay invisible in the field, and anything that needs to be
        // seen has to be surfaced in the app or sent to the Smap server instead.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree(analytics))
        }
    }
}
