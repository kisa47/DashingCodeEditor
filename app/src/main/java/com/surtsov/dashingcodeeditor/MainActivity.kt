package com.surtsov.dashingcodeeditor

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import android.content.res.AssetManager
import android.widget.Spinner
import android.widget.TextView
import android.widget.ArrayAdapter
import kotlin.text.lowercase
import android.util.Log
import android.widget.ImageButton
import android.content.ClipboardManager
import android.widget.AdapterView
import android.widget.Switch
import android.widget.Toast
import kotlin.collections.orEmpty
import kotlin.collections.toTypedArray
import android.view.View

class MainActivity : AppCompatActivity() {

    private var needSpaces: Boolean = false
    private var needUpper: Boolean = false
    private var IHUbinaryCode: String? = null

    private var sourceIHUbinaryCode: String? = null
    private var IHUsourceCode: String? = null
    private var IHUSettingsList: List<IHUSettings> = emptyList()
    private var IHUDefaultCodes: List<IHUCodes> = emptyList()
    private var IHUSpinnerList =  mutableListOf<Spinner>()
    private var BlockTypesList: List<BlockTypes> = emptyList()

    private var SettingsFileLists: List<TypeSettings> = emptyList()

    private var SettingsList: List<IHUCodesList> = emptyList()

    private var selectedBlockType: String = ""

    private var maxCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main) // Убедитесь, что установлен правильный макет

        val BlockTypesLoad = BlockTypesLoad(resources.assets)
        BlockTypesList = BlockTypesLoad.loadSettings()

        // загружаем список файлов
        val TypeSettingsManger = TypeSettingsManger(resources.assets)
        SettingsFileLists = TypeSettingsManger.loadSettings()

        // загружаем список настроек
        val LinearLayoutFunctionsIHU = findViewById<LinearLayout>(R.id.LinearLayoutFunctionsIHU)
        SettingsList = loadSettings(BlockTypesList, SettingsFileLists)

        val FuncTableSpinner = findViewById<Spinner>(R.id.FuncTableSpinner)
        FuncTableSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                addIHUFunctions(SettingsList.filter { it.block_type == selectedBlockType }[position].table)
                reloadTableValue()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // панель выбора типа блока
        val blockTypesSpiner = findViewById<Spinner>(R.id.blockTypesSpiner)
        val types = BlockTypesList.map { it.name }
        val types_adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)

        types_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        blockTypesSpiner.adapter = types_adapter

        blockTypesSpiner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                setBlockType(position)

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        // получение объектов фронта
        val resultIHUCode = findViewById<EditText>(R.id.resultIHUCode)
        val IHUBinarySwitch = findViewById<Switch>(R.id.IHUBinarySwitch)

        val settingsManagerIHU = IHUSettingsManager(resources.assets)



        // панель выбора кодировки
        val IHUDEfaultCodesSpiner = findViewById<Spinner>(R.id.IHUDEfaultCodesSpiner)
        IHUDEfaultCodesSpiner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                applyDefaultCode()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        // кнопка скопировать измененный код в буфер обмена
        val IHUCopyButton = findViewById<Button>(R.id.IHUCopyButton)
        IHUCopyButton.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("Copied Text", resultIHUCode.text)
            clipboard.setPrimaryClip(clip)

            // Сообщаем пользователю, что текст скопирован
            Toast.makeText(this, "Текст скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
        }


        // Устанавливаем обработчик изменений текста
        val inputSourceIHUCode = findViewById<EditText>(R.id.inputSourceIHUCode)
        inputSourceIHUCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Пустой метод, оставляем пустым
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Пустой метод, оставляем пустым
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkInput(s)
            }
        })

        // кнопка очистить поле ввода
        val clearIHUSourceCode = findViewById<ImageButton>(R.id.clearIHUSourceCode)
        clearIHUSourceCode.setOnClickListener {
            inputSourceIHUCode.setText(null)
            resultIHUCode.setText(null)
            Toast.makeText(this, "Поле очищено", Toast.LENGTH_SHORT).show()
        }

        // отменить изменения
        val IHULoadingButton = findViewById<Button>(R.id.IHULoadingButton)
        IHULoadingButton.setOnClickListener {
            IHUbinaryCode = loadBinary(IHUsourceCode.toString())
            reloadTableValue()
        }

        // кнопка генерации кода
        val IHUGenerateButton = findViewById<Button>(R.id.ihuGenerateButton)
        IHUGenerateButton.setOnClickListener {
            var newCode = generateCode()
            if (!IHUBinarySwitch.isChecked)
            {
                newCode = reverseIHUCode(IHUbinaryCode.toString())
            }
            resultIHUCode.setText(newCode)
        }

        // Настройки инсетов экрана
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }




    }

    private fun setBlockType(position: Int) {
        val blockType =  BlockTypesList[position]
        Log.d("SPINNER", "Выбран элемент position: ${position} name: ${blockType}")
        setAvailableCodes(blockType.name)
        setAvailableFunctionTables(blockType.name)
        maxCount = blockType.code_length
        selectedBlockType = blockType.name
    }

    private fun setAvailableCodes(blockType: String) {
        // панель выбора дефолтной кодировки
        val defaultCodesIHU = IHULoadCodes(resources.assets)
        IHUDefaultCodes = defaultCodesIHU.loadSettings()
        val IHUDEfaultCodesSpiner = findViewById<Spinner>(R.id.IHUDEfaultCodesSpiner)
        val configurations = IHUDefaultCodes.filter { it.block_type == "default" || it.block_type == blockType }.map { it.name }
        val configurations_adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, configurations)

        configurations_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        IHUDEfaultCodesSpiner.adapter = configurations_adapter
        applyDefaultCode()
    }

    private fun setAvailableFunctionTables(blockType: String) {
        val FuncTableSpinner = findViewById<Spinner>(R.id.FuncTableSpinner)
        val tables = SettingsList.filter { it.block_type == blockType }.map{ it.name }

        val tables_adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tables)
        tables_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        FuncTableSpinner.adapter = tables_adapter
    }

    private fun generateCode(): String {
        for (spinner in IHUSpinnerList) {
            val name = spinner.tag.toString().replace("IHU_spinner_", "")
            val matchingSetting = IHUSettingsList.find { it.name == name }

            if (matchingSetting != null) {
                Log.d("MyApp", "matchingSetting = ${matchingSetting}")
                Log.d("MyApp", "setting = ${spinner.selectedItem.toString()}")
                val bits = matchingSetting.states.get(spinner.selectedItem.toString())
                Log.d("MyApp", "bits = ${bits}")
                for (bit in bits?.entries.orEmpty()) {
                    Log.d("MyApp", "bit = ${bit}")
                    IHUbinaryCode = replaceCharAtIndex(IHUbinaryCode.toString(), bit.key, bit.value.toString())
                    Log.d("MyApp", "bit ${bit.key}: ${IHUbinaryCode?.toCharArray()?.get(bit.key.toInt())}")
                }

            }
        }

        var newCode = "${IHUbinaryCode}"

        return (newCode)
    }

    // Выбор деолтного конфига
    private fun applyDefaultCode() {
        val inputSourceIHUCode = findViewById<EditText>(R.id.inputSourceIHUCode)
        val IHUDEfaultCodesSpiner = findViewById<Spinner>(R.id.IHUDEfaultCodesSpiner)
        val clearIHUSourceCode = findViewById<ImageButton>(R.id.clearIHUSourceCode)

        val name = IHUDEfaultCodesSpiner.selectedItem.toString()

        val code = IHUDefaultCodes.find { it.name == name  }

        var enabled: Boolean = true

        if (code != null && code.code != "") {
            inputSourceIHUCode.setText(code.code)
            enabled = false
        }

        inputSourceIHUCode.isEnabled = enabled
        clearIHUSourceCode.isEnabled = enabled
    }

    // Смена символов местами при расшифровке кода
    private fun replaceCharAtIndex(str: String, index: Int, replacement: String): String {
        if (index < 0 || index >= str.length) throw IndexOutOfBoundsException("Index out of bounds")
        return str.substring(0, index) + replacement + str.substring(index + 1)
    }

    // Заполнение настроек из ihu_settings.json
    private fun setSettingsIHU() {
        for (spinner in IHUSpinnerList) {
            // Получаем имя настройки из тега Spinner-а
            val name = spinner.tag.toString().replace("IHU_spinner_", "")

            Log.d("MyApp", "Spinner: ${name}")

            // Ищем соответствующую настройку
            val matchingSetting = IHUSettingsList.find { it.name == name }

            Log.d("MyApp", "matchingSetting: ${matchingSetting}")

            if (matchingSetting != null) {
                var allBitsMatch = true
                var stateKey = ""
                var stateposition = 0
                // Пробегаем по всем возможным состояниям настройки
                outerLoop@ for (stateEntry in matchingSetting.states.entries) {
                    stateKey = stateEntry.key
                    stateposition = matchingSetting.states.keys.indexOf(stateKey)
                    val bitsForState = stateEntry.value
                    Log.d("MyApp", "stateKey: ${stateKey}")
                    // Проверяем соответствие всех битов
                    allBitsMatch = true
                    for (bitEntry in bitsForState.entries) {
                        val position = bitEntry.key.toInt()
                        val expectedBitValue = bitEntry.value.toString()

                        Log.d("MyApp", "position: ${position} expectedBitValue: ${expectedBitValue}")
                        // Проверяем, соответствует ли бит в позиции ожидаемому значению

                        Log.d("MyApp", "position: ${position} value: ${IHUbinaryCode?.getOrNull(position)?.toString()}")
                        if (IHUbinaryCode?.getOrNull(position)?.toString() != expectedBitValue) {
                            allBitsMatch = false
                            break
                        }
                    }

                    // Если все биты совпадают, выбираем данное состояние
                    Log.d("MyApp", "allBitsMatch: ${allBitsMatch}")
                    if (allBitsMatch) {
                        Log.d("MyApp", "stateposition: ${stateposition}")
                        spinner.setSelection(stateposition)
                        break@outerLoop
                    }
                }
            }
        }
    }

    // отключение кнопок при невалидном введеном коде
    private fun disableButtons(enabled: Boolean) {
        val IHUGenerateButton = findViewById<Button>(R.id.ihuGenerateButton)
        val IHULoadingButton = findViewById<Button>(R.id.IHULoadingButton)
        val IHUCopyButton = findViewById<Button>(R.id.IHUCopyButton)

        IHUGenerateButton.isEnabled = enabled
        IHULoadingButton.isEnabled = enabled
        IHUCopyButton.isEnabled = enabled
    }

    // Функция проверки введённого текста
    private fun checkInput(text: CharSequence?) {
        text?.let {


            val onlyUpperCase = it.contains(Regex("[A-Z]"))
            val onlyLowerCase = it.contains(Regex("[a-z]"))
            val inputSourceIHUCode = findViewById<EditText>(R.id.inputSourceIHUCode)
            var enabledButtons = false

            if (it.toString().replace(" ", "").length != maxCount) {
                inputSourceIHUCode.error =
                    "Длина IHU-кода должна быть ровно 64 символа (пробелы игнорируются)"
            }
            else if (!it.matches(Regex("^[a-fA-F0-9 ]*\$"))) {
                inputSourceIHUCode.error = "IHU код должен состоять только из латинских букв a-f или A-F цифр и пробелов"
            }
            else if (onlyUpperCase && onlyLowerCase) {
                inputSourceIHUCode.error = "IHU код не допускает смесь заглавных и строчных латинских букв!"
            }
            // если кодировка валидна, то работаем
            else {
                needUpper = onlyUpperCase
                needSpaces = it.contains(" ")
                IHUbinaryCode = loadBinary(text.toString().trim())
                IHUsourceCode = text.toString()
                setSettingsIHU()
                inputSourceIHUCode.error = null
                enabledButtons = true
                sourceIHUbinaryCode = loadBinary(IHUsourceCode.toString())
            }

            disableButtons(enabledButtons)
        }
    }

    // перевод кода в бинарный вид
    private fun loadBinary(text: String): String {
        val binary_text = convertIHUCodeToBinary(text.toString().trim())
        return (binary_text)
    }

    // Конвертация кода в бинарный вид
    // подробнее https://www.drive2.ru/l/699164876647434905/
    private fun convertIHUCodeToBinary(text: String): String {

        val lower_text = text.lowercase().replace(" ", "")
        val sb = StringBuilder()
        val converted_text = buildString {
            for (i in 1 until lower_text.length step 2) {
                append("${lower_text[i]}${lower_text[i - 1]}")
            }
            if (lower_text.length % 2 != 0) {
                append(lower_text.last())
            }
        }
        for ( i in 0 until converted_text.length step 1) {
            val decimal = Character.digit(converted_text[i], 16) // Преобразуем символ в десятичное число
            val binary = String.format("%4s", Integer.toBinaryString(decimal)).replace(' ', '0') // Формируем двоичный формат с дополнением нулями
            sb.append(binary.reversed())
        }
        return sb.toString()
    }

    // обратная операция преобразования в бинарный вид
    // подробнее https://www.drive2.ru/l/699164876647434905/
    private fun reverseIHUCode(encodedText: String): String {
        val reversedChars = encodedText.chunked(4) // Деление на группы по 4 бита
            .map { chunk -> chunk.reversed() } // Обращаем каждую группу
            .joinToString(separator = "") // Соединяем обратно в единую строку

        val originalHexValues = reversedChars.chunked(4) // Группировка по 4 бита
            .map { chunk -> Integer.parseInt(chunk, 2).toString(16).padStart(1, '0').uppercase() } // Преобразуем в HEX

        var rearrangedHex = buildString {
            for (i in 0..originalHexValues.size - 1 step 2) {
                if (i + 1 < originalHexValues.size) {
                    append(originalHexValues[i + 1]) // Начинаем с второго символа
                    append(originalHexValues[i]) // Потом добавляем первый символ
                    if (needSpaces && i + 2 < originalHexValues.size){
                        append(" ")
                    }
                } else {
                    append(originalHexValues[i]) // Последний символ оставляем без пары
                }
            }
        }

        if (!needUpper) {
            rearrangedHex = rearrangedHex.lowercase()
        }
        return rearrangedHex
    }

    // класс типов блоков
    data class BlockTypes(
        val name: String,
        val code_length: Int,
    )

    // загрузка типов блоков
    class BlockTypesLoad(private val assetManager: AssetManager) {
        fun loadSettings(): List<BlockTypes> {
            val settingsJson = assetManager.open("block_types.json").bufferedReader().use { it.readText() }
            return Gson().fromJson(settingsJson, Array<BlockTypes>::class.java).toList()
        }
    }

    // класс кодировок
    data class IHUCodes(
        val name: String,
        val code: String,
        val block_type: String,
    )

    // загрузка кодировок из файла ihu_default_codes.json
    // ToDo объединитб загрузку всех классов из файлов в отдельную функцию
    class IHULoadCodes(private val assetManager: AssetManager) {
        fun loadSettings(): List<IHUCodes> {
            val settingsJson = assetManager.open("ihu_default_codes.json").bufferedReader().use { it.readText() }
            return Gson().fromJson(settingsJson, Array<IHUCodes>::class.java).toList()
        }
    }

    // класс списка кодировок для поиска
    data class IHUCodesList(
        val name: String,
        val table: List<IHUSettings>,
        val block_type: String
    )

    // класс списка значений кодировок
    data class TypeSettings(
        val name: String,
        val block_type: String,
        val file_name: String
    )
    // загрузка кодировок из файла type_settings.json
    class TypeSettingsManger(private val assetManager: AssetManager) {
        fun loadSettings(): List<TypeSettings> {
            val settingsJson = assetManager.open("type_settings.json").bufferedReader().use { it.readText() }
            return Gson().fromJson(settingsJson, Array<TypeSettings>::class.java).toList()
        }
    }

    // класс значений кодировки
    data class IHUSettings(
        val name: String,
        val description: String,
        val states: Map<String, Map<Int, Int>>
    )

    // загрузка кодировок из файла ihu_settings.json
    // ToDo объединитб загрузку всех классов из файлов в отдельную функцию
    class IHUSettingsManager(private val assetManager: AssetManager) {
        fun loadSettings(path: String): List<IHUSettings> {
            val settingsJson = assetManager.open(path).bufferedReader().use { it.readText() }
            return Gson().fromJson(settingsJson, Array<IHUSettings>::class.java).toList()
        }
    }

    private fun generateDefaultFunctions(count: Int): List<IHUSettings> {
        val settingsList = mutableListOf<IHUSettings>()
        for (i in 0 until count) {
            val setting = IHUSettings(
                name = generateDefaultSettingName(i),
                description = generateDefaultSettingName(i),
                states = mapOf("0" to mapOf(i to 0), "1" to mapOf(i to 1))
            )
            settingsList.add(setting)
        }
        return settingsList
    }

    private fun generateDefaultSettingName(position: Int): String {
        val div = position % 4
        val pint = div + 1

        var pbn = position / 4 + 1
        if (pbn % 2 != 0) {
            pbn += 1
        } else {
            pbn -= 1
        }

        return "${position.toString()} (Byte: ${pbn.toString()} Int: ${pint})"
    }

    private fun loadSettings(blocks: List<BlockTypes>, files: List<TypeSettings>): List<IHUCodesList> {
        val codesList = mutableListOf<IHUCodesList>()
        for (block in blocks) {
            for (file in files.filter { it.block_type == block.name }) {
                val settingsManagerIHU = IHUSettingsManager(resources.assets)
                val settings = settingsManagerIHU.loadSettings(file.file_name)
                val code = IHUCodesList(
                    name = file.name,
                    block_type = file.block_type,
                    table = settings
                )
                codesList.add(code)
            }
            val defaultCodeSettings = generateDefaultFunctions(block.code_length*4)
            val defaultCode = IHUCodesList(
                name = "List of bits",
                block_type = block.name,
                table = defaultCodeSettings
            )
            codesList.add(defaultCode)
        }
        return codesList
    }

    private fun reloadTableValue() {
        setSettingsIHU()
    }

    // добавоение функций кодировки в интерфейс
    private fun addIHUFunctions(IHUSettings: List<IHUSettings>) {
        IHUSettingsList = IHUSettings
        val conteiner = findViewById<LinearLayout>(R.id.LinearLayoutFunctionsIHU)
        conteiner.removeAllViews()
        IHUSpinnerList = mutableListOf<Spinner>()
        for (setting in IHUSettings) {
            // Создание горизонтального объекта
            val horizontalLayout = LinearLayout(this)
            horizontalLayout.tag = "IHU_linearlayout_${setting.name}"
            horizontalLayout.orientation = LinearLayout.HORIZONTAL
            horizontalLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // Создание имени настройки
            val textView = TextView(this)
            textView.text = setting.name
            textView.setTooltipText(setting.description)
            textView.tag = "IHU_textview_${setting.name}"
            textView.setTextAppearance(android.R.style.TextAppearance_Material_Body1)
            textView.layoutParams = LinearLayout.LayoutParams(
                0, // width = 0
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f // weight = 1f
            )

            // Создание спинера и его заполнение
            val spinner = Spinner(this)
            spinner.tag = "IHU_spinner_${setting.name}"
            val states = setting.states?.keys.orEmpty().toTypedArray()
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, states)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            spinner.layoutParams = LinearLayout.LayoutParams(
                0, // width = 0
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f, // weight = 1f
            )

            IHUSpinnerList.add(spinner)

            // Добавление айтемов во вью
            horizontalLayout.addView(textView)
            horizontalLayout.addView(spinner)
            conteiner.addView(horizontalLayout)
        }
    }
}