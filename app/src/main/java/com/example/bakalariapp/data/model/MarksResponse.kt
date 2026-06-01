package com.example.bakalariapp.data.model

import com.google.gson.annotations.SerializedName

data class MarksResponse(
    @SerializedName("Subjects") val subjects: List<Subject>
)

data class Subject(
    @SerializedName("Marks") val marks: List<Mark>,
    @SerializedName("Subject") val subjectInfo: SubjectInfo,
    @SerializedName("AverageText") val averageText: String?,
    @SerializedName("TemporaryMark") val temporaryMark: String?,
    @SerializedName("SubjectNote") val subjectNote: String?,
    @SerializedName("TemporaryMarkNote") val temporaryMarkNote: String?,
    @SerializedName("PointsOnly") val pointsOnly: Boolean,
    @SerializedName("MarkPredictionEnabled") val markPredictionEnabled: Boolean
)

data class SubjectInfo(
    @SerializedName("Id") val id: String,
    @SerializedName("Abbrev") val abbrev: String,
    @SerializedName("Name") val name: String
)

data class Mark(
    @SerializedName("MarkDate") val markDate: String,
    @SerializedName("EditDate") val editDate: String?,
    @SerializedName("Caption") val caption: String?,
    @SerializedName("Theme") val theme: String?,
    @SerializedName("MarkText") val markText: String,
    @SerializedName("TeacherId") val teacherId: String?,
    @SerializedName("Type") val type: String,
    @SerializedName("TypeNote") val typeNote: String?,
    @SerializedName("Weight") val weight: Int?,
    @SerializedName("SubjectId") val subjectId: String,
    @SerializedName("IsNew") val isNew: Boolean,
    @SerializedName("IsPoints") val isPoints: Boolean,
    @SerializedName("CalculatedMarkText") val calculatedMarkText: String?,
    @SerializedName("ClassRankText") val classRankText: String?,
    @SerializedName("Id") val id: String,
    @SerializedName("PointsText") val pointsText: String?,
    @SerializedName("MaxPoints") val maxPoints: Int?,
    @SerializedName("ConfirmedWhen") val confirmedWhen: String?,
    @SerializedName("ConfirmedBy") val confirmedBy: String?,
    @SerializedName("MarkConfirmationState") val markConfirmationState: String?
) {
    fun getDisplayText(): String {
        return if (isPoints && pointsText?.isNotBlank() == true) {
            "$markText/$maxPoints"
        } else {
            markText
        }
    }
}
