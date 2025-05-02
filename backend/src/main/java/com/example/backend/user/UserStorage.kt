package com.example.backend.user


object UserStorage {
    val users = mutableListOf<User>()

    fun generateUserId(): Int {
        return (users.maxOfOrNull { it.id } ?: 0) + 1
    }

    fun addUser(user: User) {
        users.add(user)
    }

    fun findUserById(userId: Int): User? {
        return users.find { it.id == userId }
    }
}

