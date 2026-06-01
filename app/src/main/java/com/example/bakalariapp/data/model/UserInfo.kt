package com.example.bakalariapp.data.model

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("UserUID") val userUid: String,
    @SerializedName("FullName") val fullName: String,
    @SerializedName("Class") val userClass: ClassInfo?,
    @SerializedName("SchoolOrganizationName") val schoolName: String?,
    @SerializedName("UserType") val userType: String,
    @SerializedName("UserTypeText") val userTypeText: String,
    @SerializedName("StudyYear") val studyYear: Int?
)

data class ClassInfo(
    @SerializedName("Id") val id: String,
    @SerializedName("Abbrev") val abbrev: String,
    @SerializedName("Name") val name: String?
)
