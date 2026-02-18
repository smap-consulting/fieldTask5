package org.odk.collect.android.widgets.utilities

import androidx.activity.ComponentDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import org.javarosa.core.model.Constants
import org.javarosa.core.model.condition.EvaluationContext
import org.javarosa.core.model.data.GeoShapeData
import org.javarosa.core.model.data.GeoTraceData
import org.javarosa.core.model.data.IAnswerData
import org.javarosa.form.api.FormEntryPrompt
import org.javarosa.model.xform.XPathReference
import org.javarosa.xpath.expr.XPathFuncExpr
import org.odk.collect.android.javarosawrapper.FailedValidationResult
import org.odk.collect.android.utilities.Appearances
import org.odk.collect.android.utilities.FormEntryPromptUtils
import org.odk.collect.android.widgets.utilities.AdditionalAttributes.INCREMENTAL
import org.odk.collect.android.widgets.utilities.BindAttributes.ALLOW_MOCK_ACCURACY
import org.odk.collect.androidshared.ui.DisplayString
import org.odk.collect.geo.GeoUtils.toMapPoint
import org.odk.collect.geo.geopoly.GeoPolyFragment
import org.odk.collect.geo.geopoly.GeoPolyFragment.OutputMode
import org.odk.collect.geo.geopoly.GeoPolyUtils
import org.odk.collect.maps.MapPoint
import timber.log.Timber

class GeoPolyDialogFragment(viewModelFactory: ViewModelProvider.Factory) :
    WidgetAnswerDialogFragment<GeoPolyFragment>(
        GeoPolyFragment::class,
        viewModelFactory
    ) {

    override fun onCreateFragment(prompt: FormEntryPrompt): GeoPolyFragment {
        val outputMode = when (prompt.dataType) {
            Constants.DATATYPE_GEOSHAPE -> OutputMode.GEOSHAPE
            Constants.DATATYPE_GEOTRACE -> OutputMode.GEOTRACE
            else -> throw IllegalArgumentException()
        }

        childFragmentManager.setFragmentResultListener(
            GeoPolyFragment.REQUEST_GEOPOLY,
            this
        ) { _, result ->
            val geopolyChange = result.getString(GeoPolyFragment.RESULT_GEOPOLY_CHANGE)
            val geopoly = result.getString(GeoPolyFragment.RESULT_GEOPOLY)

            if (geopolyChange != null) {
                val incremental = FormEntryPromptUtils.getAdditionalAttribute(prompt, INCREMENTAL)
                if (incremental == "true") {
                    onValidate(geopolyChange, outputMode)
                }
            } else if (geopoly != null) {
                onAnswer(geopoly, outputMode)
            } else {
                dismiss()
            }
        }

        val retainMockAccuracy =
            FormEntryPromptUtils.getBindAttribute(prompt, ALLOW_MOCK_ACCURACY).toBoolean()

        val inputPolygon = when (val answer = prompt.answerValue) {
            is GeoTraceData -> answer.points.map { it.toMapPoint() }
            is GeoShapeData -> answer.points.map { it.toMapPoint() }
            null -> emptyList()
            else -> throw IllegalArgumentException()
        }

        val previousPolygons = getPreviousPolygons(prompt)

        return GeoPolyFragment(
            { (requireDialog() as ComponentDialog).onBackPressedDispatcher },
            outputMode,
            prompt.isReadOnly,
            retainMockAccuracy,
            inputPolygon,
            constraintValidationResult.map {
                if (it is FailedValidationResult) {
                    if (it.customErrorMessage != null) {
                        DisplayString.Raw(it.customErrorMessage)
                    } else {
                        DisplayString.Resource(it.defaultErrorMessage)
                    }
                } else {
                    null
                }
            },
            previousPolygons
        )
    }

    private fun getPreviousPolygons(prompt: FormEntryPrompt): List<List<MapPoint>> {
        if (!Appearances.hasAppearance(prompt, Appearances.HISTORY_MAP)) {
            return emptyList()
        }

        try {
            val formController = getFormController() ?: return emptyList()
            val formDef = formController.getFormDef() ?: return emptyList()
            val formInstance = formDef.instance
            val questionPath = prompt.formElement.bind.reference.toString()

            val pathExpr = XPathReference.getPathExpr(questionPath)
            val ec = EvaluationContext(formDef.evaluationContext, formInstance.root.ref)
            val nodeset = pathExpr.eval(formInstance, ec)

            val count = nodeset.size()
            if (count < 2) return emptyList()

            val result = mutableListOf<List<MapPoint>>()
            for (i in 0 until count - 1) {
                val value = nodeset.getValAt(i)
                val valueStr = if (value != null) XPathFuncExpr.toString(value) else continue
                if (valueStr.isBlank()) continue
                val points = GeoPolyUtils.parseGeometry(valueStr)
                if (points.isNotEmpty()) {
                    result.add(points)
                }
            }
            return result
        } catch (e: Exception) {
            Timber.w(e, "Failed to get previous polygons for history-map")
            return emptyList()
        }
    }

    private fun onValidate(geoString: String, outputMode: OutputMode) {
        val answer = getAnswerData(geoString, outputMode)
        onValidate(answer)
    }

    private fun onAnswer(geoString: String, outputMode: OutputMode) {
        val answer = getAnswerData(geoString, outputMode)
        onAnswer(answer)
    }

    private fun getAnswerData(geoString: String, outputMode: OutputMode): IAnswerData? {
        return if (geoString.isBlank()) {
            null
        } else {
            when (outputMode) {
                OutputMode.GEOTRACE -> GeoTraceData().also { it.value = geoString }
                OutputMode.GEOSHAPE -> GeoShapeData().also { it.value = geoString }
            }
        }
    }
}
