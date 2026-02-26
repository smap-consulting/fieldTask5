package org.odk.collect.android.formentry.audit

import org.javarosa.form.api.FormEntryController
import org.javarosa.xpath.XPathException
import org.odk.collect.android.javarosawrapper.FormController
import org.odk.collect.android.javarosawrapper.FormControllerExt.getQuestionPrompts
import org.odk.collect.android.javarosawrapper.RepeatsInFieldListException
import timber.log.Timber

object AuditUtils {
    @JvmStatic
    fun logCurrentScreen(
        formController: FormController,
        auditEventLogger: AuditEventLogger,
        currentTime: Long
    ) {
        if (formController.getEvent() == FormEntryController.EVENT_QUESTION ||
            formController.getEvent() == FormEntryController.EVENT_GROUP ||
            formController.getEvent() == FormEntryController.EVENT_REPEAT
        ) {
            try {
                for (question in formController.getQuestionPrompts()) {
                    val answer = try {
                        question.answerValue?.displayText
                    } catch (e: XPathException) {
                        Timber.w(e, "Could not get answer for audit log: %s", question.index)
                        null
                    }

                    auditEventLogger.logEvent(
                        AuditEvent.AuditEventType.QUESTION,
                        question.index,
                        true,
                        answer,
                        currentTime,
                        null
                    )
                }
            } catch (e: RepeatsInFieldListException) {
                // ignore
            }
        }
    }
}
