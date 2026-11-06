package com.spendwise.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.AppDatabase
import com.spendwise.app.data.local.entity.User
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val expenseDao = AppDatabase.getDatabase(application).expenseDao()
    private val budgetGoalDao = AppDatabase.getDatabase(application).budgetGoalDao()
    private val userPreferences = UserPreferences(application)

    private val _authState = MutableLiveData<Event<Result<User>>>()
    /** Result of the latest sign-in / register attempt. Each result is delivered once (see [Event]). */
    val authState: LiveData<Event<Result<User>>> = _authState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun signIn(email: String, pass: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    userDao.getUserByEmail(email.lowercase().trim())
                }
                if (user != null && BCrypt.checkpw(pass, user.passwordHash)) {
                    _authState.value = Event(Result.success(user))
                } else if (user == null) {
                    _authState.value = Event(Result.failure(Exception("No account found with this email")))
                } else {
                    _authState.value = Event(Result.failure(Exception("Incorrect password. Please try again.")))
                }
            } catch (e: Exception) {
                _authState.value = Event(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, pass: String, name: String, currency: String, budget: Double) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val lowercaseEmail = email.lowercase().trim()
                val existingUser = withContext(Dispatchers.IO) {
                    userDao.getUserByEmail(lowercaseEmail)
                }
                if (existingUser != null) {
                    _authState.value = Event(Result.failure(Exception("User already exists")))
                } else {
                    val hashedPassword = withContext(Dispatchers.IO) {
                        BCrypt.hashpw(pass, BCrypt.gensalt())
                    }
                    val newUser = User(
                        name = name,
                        email = lowercaseEmail,
                        passwordHash = hashedPassword,
                        preferredCurrency = currency,
                        monthlyBudget = budget
                    )
                    withContext(Dispatchers.IO) {
                        userDao.insertUser(newUser)
                    }
                    _authState.value = Event(Result.success(newUser))
                }
            } catch (e: Exception) {
                _authState.value = Event(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(newName: String) {
        viewModelScope.launch {
            val email = userPreferences.userEmail
            val user = withContext(Dispatchers.IO) {
                userDao.getUserByEmail(email)
            }
            user?.let {
                val updated = it.copy(name = newName)
                withContext(Dispatchers.IO) {
                    userDao.updateUser(updated)
                }
                userPreferences.userName = newName
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String): LiveData<Result<Unit>> {
        val result = MutableLiveData<Result<Unit>>()
        viewModelScope.launch {
            val email = userPreferences.userEmail
            val user = withContext(Dispatchers.IO) {
                userDao.getUserByEmail(email)
            }
            if (user == null) {
                result.value = Result.failure(Exception("User not found"))
                return@launch
            }
            if (!BCrypt.checkpw(currentPassword, user.passwordHash)) {
                result.value = Result.failure(Exception("Current password is incorrect"))
                return@launch
            }
            val newHash = withContext(Dispatchers.IO) {
                BCrypt.hashpw(newPassword, BCrypt.gensalt())
            }
            withContext(Dispatchers.IO) {
                userDao.updateUser(user.copy(passwordHash = newHash))
            }
            result.value = Result.success(Unit)
        }
        return result
    }

    /**
     * Deletes the signed-in user's expenses, budget goals and account, then calls [onDone]
     * on the main thread once everything is gone (so callers can safely leave the screen).
     */
    fun deleteAccount(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val email = userPreferences.userEmail
            val user = withContext(Dispatchers.IO) {
                userDao.getUserByEmail(email)
            }
            user?.let {
                withContext(Dispatchers.IO) {
                    expenseDao.deleteAllExpensesForUser(email)
                    budgetGoalDao.deleteAllGoalsForUser(email)
                    userDao.deleteUser(it)
                }
                userPreferences.clearSession()
            }
            onDone()
        }
    }

    fun signOut() {
        userPreferences.clearSession()
    }
}
