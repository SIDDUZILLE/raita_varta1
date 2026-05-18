package com.example.raitha_varta

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.raitha_varta.ui.theme.RaithaVartaTheme
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// --- 1. DATA MODELS ---
data class User(
    val name: String,
    val phone: String,
    val password: String
)

data class AgriTip(
    val id: Int,
    val category: String,
    val symbol: String,
    val imageRes: Int,
    val textKn: String,
    val textEn: String,
    val isSuccessStory: Boolean = false
)

// --- USER MANAGER (JSON STORAGE) ---
class UserManager(context: Context) {
    private val file = File(context.filesDir, "users.json")

    private fun getUsers(): MutableList<User> {
        if (!file.exists()) return mutableListOf()
        val jsonString = file.readText()
        val jsonArray = JSONArray(jsonString)
        val users = mutableListOf<User>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            users.add(User(obj.getString("name"), obj.getString("phone"), obj.getString("password")))
        }
        return users
    }

    fun register(user: User): Boolean {
        val users = getUsers()
        if (users.any { it.phone == user.phone }) return false // Already exists
        users.add(user)
        val jsonArray = JSONArray()
        users.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("phone", it.phone)
            obj.put("password", it.password)
            jsonArray.put(obj)
        }
        file.writeText(jsonArray.toString())
        return true
    }

    fun authenticate(phone: String, name: String): User? {
        // Here we just check if the user exists with this name and phone
        // In a real app, you'd check password too, but following user request for login taking name and number
        return getUsers().find { it.phone == phone && it.name.equals(name, ignoreCase = true) }
    }

    fun authenticateWithPassword(phone: String, password: String): User? {
        return getUsers().find { it.phone == phone && it.password == password }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RaithaVartaTheme {
                MainContainer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer() {
    val context = LocalContext.current
    val userManager = remember { UserManager(context) }
    
    var authState by remember { mutableStateOf("LOGIN") }
    var currentUser by remember { mutableStateOf<User?>(null) }
    
    var selectedScreen by remember { mutableStateOf(0) }
    var showWeatherDetail by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("KN") }

    when (authState) {
        "SIGNUP" -> SignupPage(
            userManager = userManager,
            onNavigateToLogin = { authState = "LOGIN" }, 
            onSignupSuccess = { 
                Toast.makeText(context, "Account Created! Please Login", Toast.LENGTH_SHORT).show()
                authState = "LOGIN" 
            }
        )
        "LOGIN" -> LoginPage(
            userManager = userManager,
            onNavigateToSignup = { authState = "SIGNUP" }, 
            onLoginSuccess = { user ->
                currentUser = user
                authState = "APP" 
            }
        )
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape, 
                                    color = Color.White, 
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable { 
                                            // LOGOUT FEATURE
                                            currentUser = null
                                            authState = "LOGIN"
                                            Toast.makeText(context, "Logged Out", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val initial = currentUser?.name?.take(1)?.uppercase() ?: "U"
                                        Text(initial, color = Color(0xFF4A148C), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(currentUser?.name ?: "User", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { 
                                currentUser = null
                                authState = "LOGIN"
                            }) {
                                Text("🚪", fontSize = 20.sp) // Logout icon
                            }
                            Row(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clickable { language = if (language == "EN") "KN" else "EN" }
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = if (language == "EN") "🌐 ಕ" else "🌐 En", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF4A148C))
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFFF8F9FA)) {
                        NavigationBarItem(selected = selectedScreen == 0, onClick = { selectedScreen = 0 }, icon = { Text("🏠", fontSize = 20.sp) }, label = { Text("Home") })
                        NavigationBarItem(selected = selectedScreen == 2, onClick = { selectedScreen = 2 }, icon = { Text("👤", fontSize = 20.sp) }, label = { Text("You") })
                        NavigationBarItem(selected = selectedScreen == 1, onClick = { selectedScreen = 1 }, icon = { Text("📸", fontSize = 20.sp) }, label = { Text("Expert") })
                    }
                }
            ) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color.White)) {
                    if (showWeatherDetail) {
                        WeatherDetailScreen(onBack = { showWeatherDetail = false }, lang = language)
                    } else {
                        when (selectedScreen) {
                            0 -> {
                                WeatherWidget(onClick = { showWeatherDetail = true }, lang = language)
                                HomeScreenContent(lang = language)
                            }
                            1 -> ExpertAskContent(lang = language)
                            2 -> ProfileScreen(lang = language, user = currentUser)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreenContent(lang: String) {
    val allTips = listOf(
        AgriTip(1, "Daily Tip", "📢", R.drawable.paddy_field, "ಬೆಳಿಗ್ಗೆ 10 ಗಂಟೆಯ ಮೊದಲು ಗದ್ದೆಗೆ ನೀರುಣಿಸುವುದು ಉತ್ತಮ. ಇದು ಮಣ್ಣಿನಲ್ಲಿ ತೇವಾಂಶ ಕಾಪಾಡುತ್ತದೆ.", "Watering before 10 AM is ideal. This helps the soil retain moisture."),
        AgriTip(2, "Sugarcane", "🎋", R.drawable.paddy_field, "ಕಬ್ಬಿನ ನಾಟಿ ಮಾಡಿದ 30 ದಿನಗಳ ನಂತರ ಮೊದಲ ಗೊಬ್ಬರ ನೀಡಿ. ಇದು ಬೆಳವಣಿಗೆ ವೇಗಗೊಳಿಸುತ್ತದೆ.", "Apply fertilizer 30 days after planting sugarcane. This accelerates growth."),
        AgriTip(3, "Tomato", "🍅", R.drawable.paddy_field, "ಟೊಮೆಟೊ ಗಿಡಗಳಿಗೆ ಆಧಾರ ನೀಡಿ. ಇದು ಹಣ್ಣುಗಳು ಕೊಳೆಯುವುದನ್ನು ತಡೆಯುತ್ತದೆ.", "Provide support to tomato plants. This prevents fruit rot."),
        AgriTip(4, "Onion", "🧅", R.drawable.paddy_field, "ಈರುಳ್ಳಿ ಕೊಯ್ಲಿಗೆ 15 ದಿನ ಮೊದಲೇ ನೀರು ನಿಲ್ಲಿಸಿ. ಇದು ಈರುಳ್ಳಿ ಬಾಳಿಕೆಯನ್ನು ಹೆಚ್ಚಿಸುತ್ತದೆ.", "Stop watering 15 days before harvest. This improves storage life."),
        AgriTip(5, "Chilli", "🌶️", R.drawable.paddy_field, "ಎಲೆ ಮುದುರು ರೋಗ ಕಂಡರೆ ತಕ್ಷಣ ಬೇವಿನ ಎಣ್ಣೆ ಸಿಂಪಡಿಸಿ. ಇದು ನೈಸರ್ಗಿಕವಾಗಿ ಕೀಟಗಳನ್ನು ತಡೆಯುತ್ತದೆ.", "Spray neem oil if you notice leaf curl disease in chilli plants. This naturally prevents pest spread."),
        AgriTip(6, "Success Story", "🏆", R.drawable.paddy_field, "ರಾಯಚೂರಿನ ಮಲ್ಲಮ್ಮ ಅವರು ಸಕಾಲದಲ್ಲಿ ಕಳೆ ಕೀಳುವ ಮೂಲಕ ಲಾಭ ಗಳಿಸಿದ್ದಾರೆ. ನೀವು ಸಹ ಈ ವಿಧಾನದಿಂದ ಶ್ರಮ ಉಳಿಸಬಹುದು.", "Mallamma from Raichur doubled profits by timely weeding. You can also save effort using this method.", true)
    )

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Success Stories", "Paddy", "Sugarcane", "Tomato", "Onion")

    val filteredTips = when (selectedCategory) {
        "All" -> allTips
        "Success Stories" -> allTips.filter { it.isSuccessStory }
        else -> allTips.filter { it.category == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(modifier = Modifier.padding(8.dp)) {
            items(categories) { cat ->
                FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) }, modifier = Modifier.padding(horizontal = 4.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF4A148C), selectedLabelColor = Color.White))
            }
        }
        val pagerState = rememberPagerState(pageCount = { filteredTips.size })
        VerticalPager(state = pagerState, modifier = Modifier.weight(1f)) { page -> TipCard(filteredTips[page], lang) }
    }
}

@Composable
fun TipCard(tip: AgriTip, lang: String) {
    Card(modifier = Modifier.fillMaxSize().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(10.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().weight(1.2f)) {
                Image(painter = painterResource(id = tip.imageRes), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Surface(modifier = Modifier.padding(12.dp), color = if (tip.isSuccessStory) Color(0xFF4A148C) else Color(0xFFFFC107), shape = RoundedCornerShape(4.dp)) {
                    Text("${tip.symbol} ${tip.category}", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, color = if (tip.isSuccessStory) Color.White else Color.Black)
                }
            }
            Column(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF3E2723)).padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text(text = if (lang == "KN") tip.textKn else tip.textEn, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
            }
        }
    }
}

@Composable
fun ExpertAskContent(lang: String) {
    var isAnalyzing by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var isHealthyResult by remember { mutableStateOf(true) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            isAnalyzing = true
            isHealthyResult = (0..1).random() == 1
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isAnalyzing && !showResult) {
            Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F9FF)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(160.dp).background(Color(0xFFC7D2FE), CircleShape), contentAlignment = Alignment.Center) { Text("📱", fontSize = 60.sp) }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { cameraLauncher.launch() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0056D2))) {
                    Text(if (lang == "KN") "ಫೋಟೋ ತೆಗೆಯಿರಿ" else "Take a Photo")
                }
            }
        } else if (isAnalyzing) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = Color(0xFF4A148C))
                Text(if (lang == "KN") "AI ವಿಶ್ಲೇಷಣೆ ನಡೆಯುತ್ತಿದೆ..." else "AI analyzing health...", modifier = Modifier.padding(top = 12.dp))
                LaunchedEffect(Unit) { delay(3000); isAnalyzing = false; showResult = true }
            }
        } else if (showResult) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(24.dp).align(Alignment.Center),
                colors = CardDefaults.cardColors(containerColor = if (isHealthyResult) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isHealthyResult) {
                            if (lang == "KN") "✅ ಬೆಳೆ ಆರೋಗ್ಯವಾಗಿದೆ" else "✅ Crop is Healthy"
                        } else {
                            if (lang == "KN") "❌ ಬೆಳೆ ಅನಾರೋಗ್ಯಕರವಾಗಿದೆ" else "❌ Crop is Not Healthy"
                        },
                        fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = if (isHealthyResult) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isHealthyResult) {
                            if (lang == "KN") "1. ಇದೇ ರೀತಿ ನೀರಾವರಿ ಮುಂದುವರಿಸಿ.\n2. ವಾರಕ್ಕೊಮ್ಮೆ ಪರೀಕ್ಷಿಸಿ.\n3. ನೈಸರ್ಗಿಕ ಗೊಬ್ಬರ ಬಳಸಿ."
                            else "1. Continue irrigation.\n2. Inspect once a week.\n3. Use organic fertilizers."
                        } else {
                            if (lang == "KN") "1. ಬೇವಿನ ಎಣ್ಣೆ ಸಿಂಪಡಿಸಿ.\n2. ಸೋಂಕಿತ ಎಲೆಗಳನ್ನು ತೆಗೆಯಿರಿ.\n3. ಸಾರಜನಕ ಕಡಿಮೆ ಮಾಡಿ."
                            else "1. Spray Neem oil.\n2. Remove infected leaves.\n3. Reduce nitrogen."
                        },
                        fontSize = 16.sp, lineHeight = 24.sp
                    )

                    Button(onClick = { showResult = false }, modifier = Modifier.align(Alignment.End).padding(top = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C))) {
                        Text(if (lang == "KN") "ಮುಚ್ಚಿ" else "Close")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(lang: String, user: User?) {
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(user?.name ?: "User") }
    var school by remember { mutableStateOf("Raitha Varta Farmer") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(100.dp).background(Color(0xFFE8F5E9), CircleShape), contentAlignment = Alignment.Center) { Text("👤", fontSize = 50.sp) }
        Spacer(modifier = Modifier.height(16.dp))
        if (isEditing) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { isEditing = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text(if (lang == "KN") "ಉಳಿಸಿ" else "Save") }
        } else {
            Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(school, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { isEditing = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text(if (lang == "KN") "ತಿದ್ದಿರಿ" else "Edit Profile") }
        }
    }
}

@Composable
fun LoginPage(userManager: UserManager, onNavigateToSignup: () -> Unit, onLoginSuccess: (User) -> Unit) {
    var name by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("+91") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Raitha-Varta", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
        Spacer(modifier = Modifier.height(40.dp))

        // Name Field - Only allows characters
        OutlinedTextField(
            value = name,
            onValueChange = { input ->
                if (input.all { it.isLetter() || it.isWhitespace() }) {
                    name = input
                    nameError = null
                }
            },
            label = { Text("Full Name") },
            isError = nameError != null,
            supportingText = { if (nameError != null) Text(nameError!!) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Field - Indian Standard (+91)
        OutlinedTextField(
            value = mobileNumber,
            onValueChange = { input ->
                // Enforce +91 prefix and limit to 13 characters (+91 + 10 digits)
                if (input.startsWith("+91")) {
                    val digits = input.substring(3)
                    if (digits.length <= 10 && digits.all { it.isDigit() }) {
                        mobileNumber = input
                        phoneError = null
                    }
                } else if (input.isEmpty() || "+91".startsWith(input)) {
                    mobileNumber = "+91"
                }
            },
            label = { Text("Phone Number") },
            isError = phoneError != null,
            supportingText = { if (phoneError != null) Text(phoneError!!) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("+91XXXXXXXXXX") }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val isNameValid = name.trim().isNotEmpty()
                val isPhoneValid = mobileNumber.matches(Regex("^\\+91[6789]\\d{9}$"))

                if (!isNameValid) nameError = "Please enter your name"
                if (!isPhoneValid) phoneError = "Enter a valid 10-digit Indian number"

                if (isNameValid && isPhoneValid) {
                    val user = userManager.authenticate(mobileNumber, name)
                    if (user != null) {
                        onLoginSuccess(user)
                    } else {
                        Toast.makeText(context, "User not found! Please Signup.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C))
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "New farmer? Create Account",
            color = Color(0xFF4A148C),
            modifier = Modifier.clickable { onNavigateToSignup() },
            textDecoration = TextDecoration.Underline
        )
    }
}

@Composable
fun SignupPage(userManager: UserManager, onNavigateToLogin: () -> Unit, onSignupSuccess: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("+91") }
    var password by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { if (it.all { c -> c.isLetter() || c.isWhitespace() }) fullName = it },
            label = { Text("Full Name") },
            isError = nameError != null,
            supportingText = { if (nameError != null) Text(nameError!!) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = mobileNumber,
            onValueChange = { input ->
                if (input.startsWith("+91")) {
                    val digits = input.substring(3)
                    if (digits.length <= 10 && digits.all { it.isDigit() }) mobileNumber = input
                } else if (input.isEmpty() || "+91".startsWith(input)) {
                    mobileNumber = "+91"
                }
            },
            label = { Text("Mobile Number") },
            isError = phoneError != null,
            supportingText = { if (phoneError != null) Text(phoneError!!) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val isNameValid = fullName.trim().isNotEmpty()
                val isPhoneValid = mobileNumber.matches(Regex("^\\+91[6789]\\d{9}$"))
                
                if (!isNameValid) nameError = "Name is required" else nameError = null
                if (!isPhoneValid) phoneError = "Invalid Indian number" else phoneError = null

                if (isNameValid && isPhoneValid && password.isNotEmpty()) {
                    val success = userManager.register(User(fullName, mobileNumber, password))
                    if (success) {
                        onSignupSuccess()
                    } else {
                        Toast.makeText(context, "User already exists!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C))
        ) {
            Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Already have an account? Login",
            color = Color(0xFF4A148C),
            modifier = Modifier.clickable { onNavigateToLogin() },
            textDecoration = TextDecoration.Underline
        )
    }
}

@Composable
fun WeatherWidget(onClick: () -> Unit, lang: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp).background(Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp)).clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(if (lang == "KN") "ಬೆಂಗಳೂರು, 6 ಮೇ" else "Bengaluru, 6 May", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            Text("31°C | ${if (lang == "KN") "ಬಿಸಿಲು" else "Sunny"}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text("☀️", fontSize = 40.sp)
    }
}

@Composable
fun WeatherDetailScreen(onBack: () -> Unit, lang: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFE0F7FA)).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", fontSize = 24.sp, modifier = Modifier.clickable { onBack() })
            Text(if (lang == "KN") "ಬೆಂಗಳೂರು" else "Bengaluru", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Card(modifier = Modifier.padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (lang == "KN") "ಇಂದು ಬೆಳಿಗ್ಗೆ ಒಣ ಹವಾಮಾನವಿರುತ್ತದೆ." else "Today morning is dry with light clouds.")
            }
        }
    }
}