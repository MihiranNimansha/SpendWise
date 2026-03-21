package com.spendwise.app.data.repository

import com.spendwise.app.data.local.dao.UserDao
import com.spendwise.app.data.local.entity.User
import org.mindrot.jbcrypt.BCrypt

class UserRepository(private val userDao: UserDao) {

    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    suspend fun getUserById(userId: Int): User? {
        return userDao.getUserById(userId)
    }

    suspend fun insertUser(user: User): Long {
        return userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val user = userDao.getUserByEmail(email.lowercase().trim())
                ?: return Result.failure(Exception("No account found with this email"))
            
            val passwordMatches = BCrypt.checkpw(password, user.passwordHash)
            if (!passwordMatches) {
                return Result.failure(Exception("Incorrect password. Please try again."))
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }
}
