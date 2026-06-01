package com.example.bakalariapp.data.model

import com.google.gson.annotations.SerializedName

data class AbsenceResponse(
    @SerializedName("PercentageThreshold") val percentageThreshold: Double,
    @SerializedName("Absences") val absences: List<AbsenceDay>,
    @SerializedName("AbsencesPerSubject") val absencesPerSubject: List<AbsencePerSubject>
)

data class AbsenceDay(
    @SerializedName("Date") val date: String,
    @SerializedName("Unsolved") val unsolved: Int,
    @SerializedName("Ok") val ok: Int,
    @SerializedName("Missed") val missed: Int,
    @SerializedName("Late") val late: Int,
    @SerializedName("Soon") val soon: Int,
    @SerializedName("School") val school: Int,
    @SerializedName("DistanceTeaching") val distanceTeaching: Int
)

data class AbsencePerSubject(
    @SerializedName("SubjectName") val subjectName: String,
    @SerializedName("LessonsCount") val lessonsCount: Int,
    @SerializedName("Base") val base: Int,
    @SerializedName("Late") val late: Int,
    @SerializedName("Soon") val soon: Int,
    @SerializedName("School") val school: Int,
    @SerializedName("DistanceTeaching") val distanceTeaching: Int
) {
    fun getAbsencePercentage(): Double {
        return if (lessonsCount > 0) {
            (base.toDouble() / lessonsCount.toDouble()) * 100.0
        } else {
            0.0
        }
    }
}
