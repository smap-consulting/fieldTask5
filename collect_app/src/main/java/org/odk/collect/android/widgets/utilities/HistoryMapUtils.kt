package org.odk.collect.android.widgets.utilities

import org.javarosa.core.model.condition.EvaluationContext
import org.javarosa.form.api.FormEntryPrompt
import org.javarosa.model.xform.XPathReference
import org.javarosa.xpath.expr.XPathFuncExpr
import org.odk.collect.android.javarosawrapper.FormController
import org.odk.collect.android.utilities.Appearances
import timber.log.Timber

// smap - shared "history-map" helper
object HistoryMapUtils {

    /**
     * Returns the answer strings of all previous instances of a repeated geometry
     * question (excluding the current/last instance) when the question has the
     * "history-map" appearance. Used to show previously collected locations on the
     * map while entering a new one.
     */
    @JvmStatic
    fun getPreviousGeometryValues(
        formController: FormController?,
        prompt: FormEntryPrompt
    ): List<String> {
        if (!Appearances.hasAppearance(prompt, Appearances.HISTORY_MAP)) {
            return emptyList()
        }

        return try {
            val formDef = formController?.getFormDef() ?: return emptyList()
            val formInstance = formDef.instance
            val questionPath = prompt.formElement.bind.reference.toString()

            val pathExpr = XPathReference.getPathExpr(questionPath)
            val ec = EvaluationContext(formDef.evaluationContext, formInstance.root.ref)
            val nodeset = pathExpr.eval(formInstance, ec)

            val count = nodeset.size()
            if (count < 2) {
                return emptyList()
            }

            val result = mutableListOf<String>()
            for (i in 0 until count - 1) {
                val value = nodeset.getValAt(i) ?: continue
                val valueStr = XPathFuncExpr.toString(value)
                if (valueStr.isNotBlank()) {
                    result.add(valueStr)
                }
            }
            result
        } catch (e: Exception) {
            Timber.w(e, "Failed to get previous geometries for history-map")
            emptyList()
        }
    }
}
