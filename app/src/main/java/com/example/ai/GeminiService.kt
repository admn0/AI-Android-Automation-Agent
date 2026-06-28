package com.example.ai

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiService {
    private val apiService: GeminiApiService

    init {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        apiService = retrofit.create(GeminiApiService::class.java)
    }

    suspend fun chatWithAgent(
        chatHistory: List<Content>,
        currentPrompt: String,
        screenContent: String? = null
    ): GenerateContentResponse? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }

        // Setup tools for AI system automation
        val tools = listOf(
            Tool(
                functionDeclarations = listOf(
                    FunctionDeclaration(
                        name = "open_application",
                        description = "Open an Android application by its Package Name.",
                        parameters = FunctionParameters(
                            properties = mapOf(
                                "package_name" to FunctionProperty("STRING", "The unique package identifier of the app, e.g., com.whatsapp, com.android.settings.")
                            ),
                            required = listOf("package_name")
                        )
                    ),
                    FunctionDeclaration(
                        name = "click_screen_element",
                        description = "Click an element visible on the screen by its text content or accessibility ID/label.",
                        parameters = FunctionParameters(
                            properties = mapOf(
                                "target" to FunctionProperty("STRING", "The text on the button or the ID of the UI element to click.")
                            ),
                            required = listOf("target")
                        )
                    ),
                    FunctionDeclaration(
                        name = "input_text_field",
                        description = "Type text into a target text field on the screen.",
                        parameters = FunctionParameters(
                            properties = mapOf(
                                "target" to FunctionProperty("STRING", "The label, placeholder, or ID of the text field."),
                                "text" to FunctionProperty("STRING", "The text content to be written.")
                            ),
                            required = listOf("target", "text")
                        )
                    ),
                    FunctionDeclaration(
                        name = "read_screen_content",
                        description = "Retrieve all visible texts and elements on the current Android screen.",
                        parameters = FunctionParameters(
                            properties = emptyMap(),
                            required = emptyList()
                        )
                    ),
                    FunctionDeclaration(
                        name = "send_whatsapp_message",
                        description = "Send a WhatsApp message directly to a specified contact.",
                        parameters = FunctionParameters(
                            properties = mapOf(
                                "contact" to FunctionProperty("STRING", "The name of the contact, or phone number."),
                                "message" to FunctionProperty("STRING", "The message payload text.")
                            ),
                            required = listOf("contact", "message")
                        )
                    ),
                    FunctionDeclaration(
                        name = "create_automation_workflow",
                        description = "Create a complete, connected multi-node automation workflow visually. Use this when the user describes an automation sequence like 'When WhatsApp message with bill arrives, save file and TTS speak' or any trigger-action chain.",
                        parameters = FunctionParameters(
                            properties = mapOf(
                                "name" to FunctionProperty("STRING", "A short descriptive name of the workflow in Arabic, e.g. 'مساعد الفواتير الذكي'"),
                                "description" to FunctionProperty("STRING", "A description of what the workflow does in Arabic."),
                                "nodes_csv" to FunctionProperty("STRING", "CSV definition of nodes to create. Format: TYPE|LABEL|X|Y|CONFIG_VALUE separated by newlines. TYPES: TRIGGER_TIME, TRIGGER_NOTIFICATION, AI_AGENT, DELAY, HTTP_REQUEST, ACTION_TTS, ACTION_OPEN_APP, ACTION_SMS. Example: TRIGGER_NOTIFICATION|مستقبل الرسائل|100|200|فاتورة\nACTION_TTS|نطق صوتي|400|200|تم استلام فاتورة جديدة"),
                                "connections_csv" to FunctionProperty("STRING", "CSV definition of connections from source node index to destination node index (0-indexed based on nodes_csv list order). Format: fromIndex|toIndex separated by newlines. Example: 0|1")
                            ),
                            required = listOf("name", "description", "nodes_csv", "connections_csv")
                        )
                    )
                )
            )
        )

        // Build system prompt in Arabic and English
        val sysInstructionText = """
            أنت مدير نظام أندرويد (AI Android Automation Agent) ووكيل أتمتة متكامل شبيه بـ n8n. مهمتك هي مساعدة المستخدم في تنفيذ المهام وإنشاء مسارات عمل أتمتة بصرية متعددة العقد (Multi-Node Workflows) برمجياً عبر (Function Calling).
            تواصل باللغة العربية باختصار تام.
            
            معلومات الشاشة الحالية:
            ${screenContent ?: "لا تتوفر لقطة شاشة حالية. يمكنك طلب قراءة الشاشة إذا لزم الأمر."}
            
            الدوال المتاحة لك للتنفيذ:
            1. open_application(package_name): تشغيل أي تطبيق مثبت.
            2. click_screen_element(target): الضغط على عنصر شاشة.
            3. input_text_field(target, text): الكتابة في حقول الإدخال.
            4. read_screen_content(): قراءة نصوص الشاشة الحالية بالكامل.
            5. send_whatsapp_message(contact, message): إرسال رسالة واتساب.
            6. create_automation_workflow(name, description, nodes_csv, connections_csv): بناء مسار عمل كامل بمرونة متناهية.
            
            عندما يطلب المستخدم أتمتة متسلسلة مثل "عند الساعة الثامنة صباحاً نبهني صوتياً بعبارة صباح الخير ثم افتح التقويم"، قم باستدعاء 'create_automation_workflow' لتصميم مسار بصري رائع يربط هذه العقد ببعضها البعض.
        """.trimIndent()

        val systemInstruction = Content(
            parts = listOf(Part(text = sysInstructionText))
        )

        // Add user prompt
        val fullContents = chatHistory.toMutableList().apply {
            add(Content(role = "user", parts = listOf(Part(text = currentPrompt))))
        }

        val request = GenerateContentRequest(
            contents = fullContents,
            tools = tools,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(temperature = 0.2f)
        )

        return try {
            apiService.generateContent(apiKey, request)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
