package com.boxbox.app.data.repository

import com.boxbox.app.data.api.RetrofitClient
import com.boxbox.app.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import android.net.Uri

class BoxBoxRepository {

    private val openF1 = RetrofitClient.openF1Api
    private val jolpica = RetrofitClient.jolpicaApi
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // ---- OpenF1 ----

    suspend fun getSessions(year: Int): List<Session> =
        openF1.getSessions(year)

    suspend fun getMeetings(year: Int): List<Meeting> =
        openF1.getMeetings(year)

    suspend fun getDrivers(sessionKey: String = "latest"): List<Driver> =
        openF1.getDrivers(sessionKey)

    suspend fun getLivePositions(): List<Position> =
        openF1.getPositions("latest")

    suspend fun getLiveRaceControl(): List<RaceControlMessage> =
        openF1.getRaceControl("latest")

    suspend fun getLiveStints(): List<Stint> =
        openF1.getStints("latest")

    suspend fun getLiveIntervals(): List<Interval> =
        openF1.getIntervals("latest")

    suspend fun getLiveLaps(): List<Lap> =
        openF1.getLaps("latest")

    // Replay - get race control for a specific past session
    suspend fun getRaceControlForSession(sessionKey: Int): List<RaceControlMessage> =
        openF1.getRaceControl(sessionKey.toString())

    suspend fun getPositionsForSession(sessionKey: Int): List<Position> =
        openF1.getPositions(sessionKey.toString())

    // ---- Jolpica ----

    suspend fun getDriverStandings(): List<DriverStanding> {
        val response = jolpica.getDriverStandings()
        return response.MRData.StandingsTable?.StandingsLists?.firstOrNull()
            ?.DriverStandings ?: emptyList()
    }

    suspend fun getConstructorStandings(): List<ConstructorStanding> {
        val response = jolpica.getConstructorStandings()
        return response.MRData.StandingsTable?.StandingsLists?.firstOrNull()
            ?.ConstructorStandings ?: emptyList()
    }

    suspend fun getCurrentSchedule(): List<Race> {
        val response = jolpica.getCurrentSchedule()
        return response.MRData.RaceTable?.Races ?: emptyList()
    }

    suspend fun getLastRaceResults(): List<Race> {
        val response = jolpica.getLastRaceResults()
        return response.MRData.RaceTable?.Races ?: emptyList()
    }

    // ---- Firebase Auth ----

    fun getCurrentUser() = auth.currentUser

    suspend fun signIn(email: String, password: String) =
        auth.signInWithEmailAndPassword(email, password).await()

    suspend fun signUp(email: String, password: String) =
        auth.createUserWithEmailAndPassword(email, password).await()

    fun signOut() = auth.signOut()

    // ---- Firebase Firestore (CRUD) ----

    // CREATE
    suspend fun createUserProfile(profile: UserProfile) {
        firestore.collection("users")
            .document(profile.uid)
            .set(profile)
            .await()
    }

    // READ
    suspend fun getUserProfile(uid: String): UserProfile? {
        val doc = firestore.collection("users")
            .document(uid)
            .get()
            .await()
        return doc.toObject(UserProfile::class.java)
    }

    // UPDATE
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>) {
        firestore.collection("users")
            .document(uid)
            .update(updates)
            .await()
    }

    // DELETE
    suspend fun deleteUserProfile(uid: String) {
        firestore.collection("users")
            .document(uid)
            .delete()
            .await()
        auth.currentUser?.delete()?.await()
    }

    // ---- Firebase Storage (profile photo) ----

    suspend fun uploadProfilePhoto(uid: String, uri: Uri): String {
        val ref = storage.reference.child("profile_photos/$uid.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteProfilePhoto(uid: String) {
        storage.reference.child("profile_photos/$uid.jpg").delete().await()
    }
}
