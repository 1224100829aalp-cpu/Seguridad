# Flujo de Autenticación - Guía Didáctica

## 🎯 Objetivo de Aprendizaje
Comprender paso a paso cómo funciona el sistema de autenticación desde que el usuario
abre la app hasta que cierra sesión.

---

## 🟢 Fase 1: Apertura de la App (Splash Screen)

```kotlin
// SplashScreen.kt
val isLoggedIn = viewModel.isLoggedIn()
if (isLoggedIn) {
    viewModel.validateToken()
    onNavigateToHome()
} else {
    onNavigateToLogin()
}

// Flujo de Datos
/*
Usuario abre app
↓
SplashScreen
↓
AuthViewModel.isLoggedIn()
↓
AuthRepository.isLoggedIn()
↓
SecureStorage verifica:
- ¿Hay token guardado?
- ¿La sesión no ha expirado?
↓
SI → Home Screen
NO → Login Screen
*/

// LoginScreen.kt
LoadingButton(
    text = "Iniciar Sesión",
    onClick = { performLogin() },
    isLoading = authState is AuthState.Loading
)

fun validateFields(): Boolean {
    if (email.isBlank()) {
        emailError = "El email es obligatorio"
        return false
    }
    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        emailError = "Formato inválido"
        return false
    }
    if (password.length < 6) {
        passwordError = "Mínimo 6 caracteres"
        return false
    }
    return true
}

fun performLogin() {
    if (validateFields()) {
        viewModel.login(email, password)
    }
}

// AuthViewModel.kt
fun login(email: String, password: String) {
    _authState.value = AuthState.Loading
    viewModelScope.launch {
        val result = repository.login(email, password)
        result.onSuccess { user ->
            _currentUser.value = user
            _authState.value = AuthState.Success(user)
        }.onFailure { exception ->
            _authState.value = AuthState.Error(exception.message)
        }
    }
}

// AuthRepository.kt
suspend fun login(email: String, password: String): Result<User> {
    return withContext(Dispatchers.IO) {
        try {
            val loginRequest = LoginRequest(email, password)
            val response = apiService.login(loginRequest)
            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()!!.user!!
                secureStorage.saveUserSession(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Credenciales incorrectas"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// SecureStorage.kt
fun saveUserSession(user: User) {
    sharedPreferences.edit().apply {
        putString(KEY_TOKEN, user.token) // ← ENCRIPTADO automáticamente
        putString(KEY_USER_ID, user.id)
        putString(KEY_USER_EMAIL, user.email)
        putString(KEY_USER_NAME, user.name)
        putBoolean(KEY_IS_LOGGED_IN, true)
        putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
        apply()
    }
}

/*
Diagrama de flujo de login:
Usuario ingresa credenciales
↓
LoginScreen valida formato
↓
AuthViewModel.login()
↓
AuthRepository.login()
↓
API Server valida credenciales
↓
¿Válido?
SI → Devuelve User + Token
↓
SecureStorage.saveUserSession() (ENCRIPTADO)
↓
AuthState.Success
↓
Navega a HomeScreen
NO → AuthState.Error
↓
Muestra ErrorDialog
*/
// HomeScreen.kt
DisposableEffect(Unit) {
    viewModel.updateUserActivity() // Actualiza timestamp
    onDispose { }
}

// Peticiones autenticadas
val token = secureStorage.getToken()
apiService.getData("Bearer $token")
// HomeScreen.kt
fun logout() {
    viewModel.logout()
}

// AuthViewModel.kt
fun logout() {
    _authState.value = AuthState.Loading
    viewModelScope.launch {
        repository.logout()
        _currentUser.value = null
        _authState.value = AuthState.Logout
    }
}

// AuthRepository.kt
suspend fun logout(): Result<Boolean> {
    return withContext(Dispatchers.IO) {
        try {
            val token = secureStorage.getToken()
            if (token != null) {
                apiService.logout("Bearer $token")
            }
            secureStorage.clearSession() // ← LIMPIEZA COMPLETA
            Result.success(true)
        } catch (e: Exception) {
            secureStorage.clearSession() // Limpiamos aunque falle
            Result.failure(e)
        }
    }
}
// SecureStorage.kt
private fun isSessionValid(): Boolean {
    val sessionTimestamp = sharedPreferences.getLong(KEY_SESSION_TIMESTAMP, 0L)
    val currentTime = System.currentTimeMillis()
    val sessionAge = currentTime - sessionTimestamp
    return sessionAge < SESSION_TIMEOUT // 24 horas
}

fun getToken(): String? {
    return if (isSessionValid()) {
        sharedPreferences.getString(KEY_TOKEN, null)
    } else {
        clearSession() // Sesión expirada
        null
    }
}
Nivel 1: Validación Cliente
- Formato de email
- Longitud de contraseña
- Campos obligatorios

Nivel 2: Transmisión Segura
- HTTPS obligatorio
- Headers de autenticación
- Timeouts configurados

Nivel 3: Almacenamiento
- EncryptedSharedPreferences
- AES-256 encryption
- MasterKey protegida

Nivel 4: Sesión
- Timeout de inactividad
- Validación periódica
- Limpieza exhaustiva

Nivel 5: Servidor (Backend)
- Validación de credenciales
- Generación segura de tokens
- Verificación de firma JWT
- Rate limiting
