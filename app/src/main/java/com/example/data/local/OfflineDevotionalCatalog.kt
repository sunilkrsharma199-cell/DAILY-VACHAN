package com.example.data.local

import com.example.data.model.DevotionalLanguage
import com.example.data.model.TimeSlot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object OfflineDevotionalCatalog {

    private data class SeedEntry(
        val verseReference: String,
        val verseText: String,
        val reflection: String,
        val shortPrayer: String,
        val moodTag: String
    )

    fun dateKeyOffset(daysOffset: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun buildDevotionalForDate(
        dateKey: String,
        slot: TimeSlot,
        language: DevotionalLanguage,
        daySeedIndex: Int
    ): DevotionalEntity {
        val entries = when (language) {
            DevotionalLanguage.ENGLISH -> if (slot == TimeSlot.MORNING) englishMorning else englishEvening
            DevotionalLanguage.HINDI -> if (slot == TimeSlot.MORNING) hindiMorning else hindiEvening
            DevotionalLanguage.HINGLISH -> if (slot == TimeSlot.MORNING) hinglishMorning else hinglishEvening
            DevotionalLanguage.TAMIL -> if (slot == TimeSlot.MORNING) tamilMorning else tamilEvening
            DevotionalLanguage.TELUGU -> if (slot == TimeSlot.MORNING) teluguMorning else teluguEvening
            DevotionalLanguage.MARATHI -> if (slot == TimeSlot.MORNING) marathiMorning else marathiEvening
        }
        val picked = entries[Math.floorMod(daySeedIndex, entries.size)]
        val id = "${dateKey}_${slot.key}_${language.code}"
        return DevotionalEntity(
            id = id,
            dateKey = dateKey,
            timeSlot = slot.key,
            languageCode = language.code,
            verseReference = picked.verseReference,
            verseText = picked.verseText,
            reflection = picked.reflection,
            shortPrayer = picked.shortPrayer,
            moodTag = picked.moodTag,
            createdAt = System.currentTimeMillis()
        )
    }

    fun dayIndexForDateKey(dateKey: String): Int {
        return try {
            val parts = dateKey.split("-")
            val y = parts[0].toInt()
            val m = parts[1].toInt()
            val d = parts[2].toInt()
            y * 372 + m * 31 + d
        } catch (_: Exception) {
            0
        }
    }

    private val englishMorning = listOf(
        SeedEntry(
            verseReference = "Lamentations 3:22–23",
            verseText = "The steadfast love of the Lord never ceases; His mercies never come to an end; they are new every morning.",
            reflection = "Before you check your phone messages or rush into today's work, pause and take one slow breath. You do not have to carry yesterday's mistakes or tomorrow's pressure into this morning. God's kindness is fresh for today—right where you are, in your home, on your commute, and in every quiet responsibility ahead.",
            shortPrayer = "Lord, thank You for this new morning. Give me a calm heart, clear steps, and patience with everyone I meet today. Amen.",
            moodTag = "Morning Grace"
        ),
        SeedEntry(
            verseReference = "Psalm 23:1–3",
            verseText = "The Lord is my shepherd; I shall not want. He makes me lie down in green pastures. He restores my soul.",
            reflection = "Life often pushes us to hurry and worry about what might run out—money, time, or energy. Yet Jesus promises to guide you gently, just like a caring shepherd who knows every turn of the road. Let your shoulders relax today. You are looked after.",
            shortPrayer = "Heavenly Father, restore my tired spirit today and help me trust Your timing in every matter. Amen.",
            moodTag = "Peace"
        ),
        SeedEntry(
            verseReference = "Isaiah 41:10",
            verseText = "Fear not, for I am with you; be not dismayed, for I am your God; I will strengthen you, I will help you.",
            reflection = "Whatever difficult conversation, bill, or decision is waiting for you today, you are not walking into it alone. Strength does not mean pretending everything is easy; it means holding God's hand while taking one faithful step at a time.",
            shortPrayer = "God, hold my hand today. Replace my fear with quiet courage and remind me You are right beside me. Amen.",
            moodTag = "Strength"
        ),
        SeedEntry(
            verseReference = "Proverbs 3:5–6",
            verseText = "Trust in the Lord with all your heart, and do not lean on your own understanding. In all your ways acknowledge Him, and He will make straight your paths.",
            reflection = "Sometimes we want the entire five-year map before we take a single step. God rarely shows the whole road at once—He lights just enough for today. Offer your plans to Him this morning and watch Him open the right doors.",
            shortPrayer = "Lord, I place my work, family, and decisions in Your hands today. Guide me gently on the right path. Amen.",
            moodTag = "Guidance"
        )
    )

    private val englishEvening = listOf(
        SeedEntry(
            verseReference = "Psalm 4:8",
            verseText = "In peace I will both lie down and sleep; for You alone, O Lord, make me dwell in safety.",
            reflection = "The day is done. Whatever stayed unfinished can wait until tomorrow morning. Hand over your unanswered questions, family worries, and tired thoughts to God tonight. Rest is also an act of faith.",
            shortPrayer = "Father, thank You for bringing me safely through today. Watch over my home tonight and give me deep, peaceful sleep. Amen.",
            moodTag = "Evening Rest"
        ),
        SeedEntry(
            verseReference = "Matthew 11:28",
            verseText = "Come to me, all who labor and are heavy laden, and I will give you rest.",
            reflection = "If your mind is still replaying work stress or heavy thoughts tonight, hear Jesus inviting you like a close friend: sit down, put the heavy bag on the floor, and breathe. You don't have to hold the world together while you sleep.",
            shortPrayer = "Jesus, I lay down every burden at Your feet tonight. Quiet my thoughts and fill my home with Your peace. Amen.",
            moodTag = "Peace"
        ),
        SeedEntry(
            verseReference = "Psalm 121:7–8",
            verseText = "The Lord will keep you from all evil; He will keep your life. The Lord will keep your going out and your coming in.",
            reflection = "As night settles over your neighborhood, remember that God never sleeps or loses watch over you and your loved ones. Thank Him for one small blessing from today before closing your eyes.",
            shortPrayer = "Lord, thank You for Your quiet protection today. Bless my family with health, warmth, and restful sleep. Amen.",
            moodTag = "Gratitude"
        )
    )

    private val hindiMorning = listOf(
        SeedEntry(
            verseReference = "विलापगीत 3:22–23",
            verseText = "प्रभु की करुणा कभी समाप्त नहीं होती, उसकी दया हर भोर को नई होती है; तेरी सच्चाई महान है।",
            reflection = "आज के काम और भागदौड़ शुरू करने से पहले एक पल रुककर गहरी सांस लें। कल की कोई भी गलती या चिंता आपको आज के साथ नहीं लानी है। हर सुबह ईश्वर की नई दया आपके साथ है—आपके घर में, आपके काम में और हर कदम पर।",
            shortPrayer = "हे प्रभु, इस नई सुबह के लिए धन्यवाद। आज मेरे मन को शांति, वाणी में मिठास और हर काम में सही समझ देना। आमीन।",
            moodTag = "सुबह की कृपा"
        ),
        SeedEntry(
            verseReference = "भजन संहिता 23:1–3",
            verseText = "प्रभु मेरा चरवाहा है, मुझे कुछ घटी न होगी। वह मुझे हरी हरी चराइयों में बैठाता है और मेरे प्राणों को विश्राम देता है।",
            reflection = "कई बार हमें भविष्य, नौकरी या घर की जिम्मेदारियों की चिंता सताती है। लेकिन परमेश्वर एक सच्चे चरवाहे की तरह आपकी हर ज़रूरत को जानते हैं। आज मन को हल्का रखें, आप अकेले नहीं हैं।",
            shortPrayer = "प्रभु, मेरी थकान और चिंता को अपनी शांति में बदल दे। आज पूरे दिन मुझे अपनी राह पर चलाना। आमीन।",
            moodTag = "शांति"
        ),
        SeedEntry(
            verseReference = "यशायाह 41:10",
            verseText = "मत डर, क्योंकि मैं तेरे संग हूं; इधर-उधर मत ताक, क्योंकि मैं तेरा परमेश्वर हूं; मैं तुझे दृढ़ करूंगा और तेरी सहायता करूंगा।",
            reflection = "आज कोई भी कठिन परिस्थिति या जरूरी बात सामने आए, याद रखें कि ईश्वर का हाथ आपके कंधे पर है। हिम्मत का मतलब यह नहीं कि डर न लगे, बल्कि डर के बीच भी विश्वास से आगे बढ़ना है।",
            shortPrayer = "हे पिता, मुझे आज भीतर से मजबूत बना। हर घबराहट को दूर कर और मेरे परिवार की रक्षा कर। आमीन।",
            moodTag = "ताकत"
        )
    )

    private val hindiEvening = listOf(
        SeedEntry(
            verseReference = "भजन संहिता 4:8",
            verseText = "मैं शांति से लेट जाऊंगा और सो भी जाऊंगा; क्योंकि हे प्रभु, केवल तू ही मुझे निडर विश्राम देता है।",
            reflection = "आज का दिन पूरा हुआ। जो काम अधूरा रह गया, वह कल संभल जाएगा। रात को सोते समय अपने सारे बोझ और सवाल ईश्वर के हाथों में सौंप दें। गहरी नींद भी विश्वास का एक रूप है।",
            shortPrayer = "प्रभु, आज दिन भर साथ रहने के लिए धन्यवाद। मेरे घर पर अपनी शांति बनाए रख और मीठी नींद दे। आमीन।",
            moodTag = "संध्या शांति"
        ),
        SeedEntry(
            verseReference = "मत्ती 11:28",
            verseText = "हे सब परिश्रम करने वालों और बोझ से दबे लोगों, मेरे पास आओ; मैं तुम्हें विश्राम दूंगा।",
            reflection = "अगर आज का दिन थकाने वाला था, तो यीशु की यह आवाज़ सुनें—अपने मन का भारी बोझ यहीं रख दें। जब आप सोते हैं, तब भी परमेश्वर जागकर आपका ध्यान रखते हैं।",
            shortPrayer = "हे यीशु, आज की सारी थकान और चिंता आपके चरणों में रखता हूं। मुझे और मेरे अपनों को सुख की नींद दें। आमीन।",
            moodTag = "विश्राम"
        )
    )

    private val hinglishMorning = listOf(
        SeedEntry(
            verseReference = "Lamentations 3:22–23",
            verseText = "Prabhu ki daya kabhi khatam nahi hoti; unki karuna har subah nayi hoti hai.",
            reflection = "Aaj ki bhaag-daud aur phone notifications check karne se pehle ek gehri saans lein. Kal ki tension ya kisi purani baat ka bojh aaj subah saath leke mat chaliye. Har nayi subah God ki nayi grace aapke saath hai—aapke kaam mein, ghar mein, aur har chhote faisle mein.",
            shortPrayer = "Thank you Prabhu is nayi subah ke liye. Aaj mere dil ko shanti, dimaag ko clarity, aur sabke saath pyaar se pesh aane ki taqat dein. Amen.",
            moodTag = "Morning Grace"
        ),
        SeedEntry(
            verseReference = "Psalm 23:1–3",
            verseText = "Prabhu mera shepherd hai, mujhe kisi cheez ki kami nahi hogi. Woh mere mann ko sukoon deta hai.",
            reflection = "Aksar hum kal ki job, paise ya family ki fikar mein aaj ka sukoon kho dete hain. Par God ek caring guide ki tarah aapki har zaroorat jaante hain. Apne shoulders relax kijiye—aap akele nahi chal rahe hain.",
            shortPrayer = "Lord, meri har chinta ko apni shanti se badal dijiye aur aaj mere har kadam ko sahi raah dikhaiye. Amen.",
            moodTag = "Peace"
        ),
        SeedEntry(
            verseReference = "Isaiah 41:10",
            verseText = "Mat daro, kyunki Main tumhare saath hoon; ghabrao mat, kyunki Main tumhara God hoon aur Main tumhe sambhalunga.",
            reflection = "Chahe aaj koi tough meeting ho, ghar ki zimmedari ho ya koi tension—aap akela feel mat kijiye. Courage ka matlab yeh nahi ki dar na lage, balki God ka haath pakad kar ek ek step aage badhna hai.",
            shortPrayer = "Father, aaj mujhe andar se strong banaiye. Dar ki jagah bharosa dijiye aur mere parivar ko sambhaliye. Amen.",
            moodTag = "Strength"
        )
    )

    private val hinglishEvening = listOf(
        SeedEntry(
            verseReference = "Psalm 4:8",
            verseText = "Main shanti se letunga aur sukoon se so jaunga, kyunki he Prabhu, sirf Tu hi mujhe surakshit rakhta hai.",
            reflection = "Aaj ka din poora hua. Jo kaam baaki reh gaya, use kal ke liye chhod dein. Raat ko sone se pehle apni saari uljhanein aur stress God ke haath mein de dijiye. Chain se sona bhi bharose ka ek tareeka hai.",
            shortPrayer = "Thank you God aaj din bhar saath rehne ke liye. Mere ghar par apni shanti rakhein aur achi neend dein. Amen.",
            moodTag = "Evening Rest"
        ),
        SeedEntry(
            verseReference = "Matthew 11:28",
            verseText = "Mere paas aao, tum sab jo thake hue aur bojh se dabe ho; Main tumhe aaram dunga.",
            reflection = "Agar aaj ka din heavy tha aur dimaag abhi bhi soch raha hai, toh Jesus ki is baat ko ek dost ki tarah sunein: apna bhaari bag neeche rakh dein aur aaram karein.",
            shortPrayer = "Jesus, aaj ki saari thakaan aur tension aapke paas rakhta hoon. Mujhe aur meri family ko sukoon ki neend dein. Amen.",
            moodTag = "Peace"
        )
    )

    private val tamilMorning = listOf(
        SeedEntry(
            verseReference = "புலம்பல் 3:22–23",
            verseText = "கர்த்தருடைய கிருபைகள் முடிவுபெறாதவைகள்; அவருடைய இரக்கங்கள் காலைதோறும் புதியவைகள்.",
            reflection = "இன்றைய வேலைகளைத் தொடங்குவதற்கு முன் ஒரு நிமிடம் அமைதியாக மூச்சு விடுங்கள். நேற்றைய கவலைகளை இன்று சுமக்க வேண்டாம். இந்தப் புதிய காலையில் கடவுளின் அன்பும் அமைதியும் உங்களோடு இருக்கிறது.",
            shortPrayer = "அன்புள்ள ஆண்டவரே, இந்தப் புதிய காலைக்காக நன்றி. இன்று எனக்கு அமைதியான மனதையும் சரியான வழிநடத்துதலையும் தாரும். ஆமென்.",
            moodTag = "காலை கிருபை"
        ),
        SeedEntry(
            verseReference = "சங்கீதம் 23:1–3",
            verseText = "கர்த்தர் என் மேய்ப்பராயிருக்கிறார்; நான் தாழ்ச்சியடையேன். அவர் என் ஆத்துமாவைத் தேற்றுகிறார்.",
            reflection = "எதிர்காலத்தைப் பற்றிய பயம் வேண்டாம். நல்ல மேய்ப்பனைப் போல இறைவன் உங்கள் குடும்பத்தையும் வேலைகளையும் அன்போடு கவனித்துக் கொள்வார்.",
            shortPrayer = "இறைவா, என் மனபாரங்களை நீக்கி இன்று முழுவதும் உம் சமாதானத்தால் என்னை நிரப்பும். ஆமென்.",
            moodTag = "சமாதானம்"
        )
    )

    private val tamilEvening = listOf(
        SeedEntry(
            verseReference = "சங்கீதம் 4:8",
            verseText = "சமாதானத்தோடே படுத்துக்கொண்டு நித்திரைசெய்வேன்; கர்த்தாவே, நீர் ஒருவரே என்னைச் சுகமாய்த் தங்கப்பண்ணுகிறீர்.",
            reflection = "இன்றைய நாள் முடிந்துவிட்டது. முடியாத வேலைகளை நாளைக்கு விட்டுவிடுங்கள். இரவில் உங்கள் கவலைகளை இறைவனிடம் ஒப்படைத்து நிம்மதியாக உறங்குங்கள்.",
            shortPrayer = "ஆண்டவரே, இன்று முழுவதும் காத்தமைக்காக நன்றி. எங்கள் இல்லத்திற்கு அமைதியான இரவைத் தாரும். ஆமென்.",
            moodTag = "மாலை அமைதி"
        )
    )

    private val teluguMorning = listOf(
        SeedEntry(
            verseReference = "విలాపవాక్యములు 3:22–23",
            verseText = "యెహోవా కృప ఎన్నటికీ తొలగిపోదు; అనుదినము నూతనముగా ఆయన వాత్సల్యత కలుగును.",
            reflection = "ఈ రోజు పనులలోకి వెళ్ళే ముందు ఒక్క క్షణం ప్రశాంతంగా ఊపిరి తీసుకోండి. నిన్నటి బాధలను ఈ రోజు మోయవలసిన అవసరం లేదు. ఈ కొత్త ఉదయంలో దేవుని కృప మీతోనే ఉంది.",
            shortPrayer = "ప్రియమైన ప్రభువా, ఈ కొత్త ఉదయానికి వందనాలు. ఈ రోజు నా మనసుకు శాంతిని, అడుగులకు సరైన దారిని దయచేయండి. ఆమెన్.",
            moodTag = "ఉదయ కృప"
        ),
        SeedEntry(
            verseReference = "కీర్తనలు 23:1–3",
            verseText = "యెహోవా నా కాపరి; నాకు లేమి కలుగదు. ఆయన నా ప్రాణమునకు సేదదీర్చుచున్నాడు.",
            reflection = "రేపటి గురించి ఆందోళన పడకండి. మంచి కాపరిలా దేవుడు మీ ప్రతి అవసరాన్ని ఎరిగి మిమ్మల్ని నడిపిస్తారు.",
            shortPrayer = "ప్రభువా, నా భయాలను తొలగించి ఈ రోజంతా మీ శాంతితో నన్ను నింపండి. ఆమెన్.",
            moodTag = "శాంతి"
        )
    )

    private val teluguEvening = listOf(
        SeedEntry(
            verseReference = "కీర్తనలు 4:8",
            verseText = "యెహోవా, నెమ్మదితో పండుకొని నిద్రపోవుదును; నీవే నన్ను సురక్షితముగా నివసింపజేయుదువు.",
            reflection = "ఈ రోజు ముగిసింది. పూర్తికాని పనులను రేపటికి వదిలేయండి. మీ భారాలన్నీ దేవుని చేతుల్లో పెట్టి ప్రశాంతంగా విశ్రమించండి.",
            shortPrayer = "తండ్రీ, ఈ రోజంతా కాపాడినందుకు వందనాలు. మా ఇంటికి ప్రశాంతమైన నిద్రను దయచేయండి. ఆమెన్.",
            moodTag = "సాయంత్రపు శాంతి"
        )
    )

    private val marathiMorning = listOf(
        SeedEntry(
            verseReference = "विलापगीत 3:22–23",
            verseText = "परमेश्वराची दया कधीही संपत नाही; तिचा प्रत्येय दर सकाळी नवा असतो.",
            reflection = "आजच्या कामाची घाई सुरू करण्यापूर्वी एक शांत श्वास घ्या. कालची चिंता किंवा ताण आजच्या दिवसात आणू नका. या नवीन सकाळी देवाची कृपा आणि शांतता तुमच्यासोबत आहे.",
            shortPrayer = "हे प्रभू, या सुंदर सकाळीबद्दल धन्यवाद. आज माझ्या मनाला शांती आणि प्रत्येक पावलावर योग्य मार्गदर्शन दे. आमेन.",
            moodTag = "सकाळची कृपा"
        ),
        SeedEntry(
            verseReference = "स्तोत्रसंहिता 23:1–3",
            verseText = "परमेश्वर माझा मेंढपाळ आहे; मला काही उणे पडणार नाही. तो माझ्या जीवाला विश्रांती देतो.",
            reflection = "कामाचा किंवा भविष्याचा ताण घेऊ नका. प्रेमळ मार्गदर्शकाप्रमाणे देव तुमची आणि तुमच्या कुटुंबाची काळजी घेत आहे.",
            shortPrayer = "प्रभू, माझी सर्व काळजी दूर कर आणि आज दिवसभर मला तुझ्या शांतीत ठेव. आमेन.",
            moodTag = "शांती"
        )
    )

    private val marathiEvening = listOf(
        SeedEntry(
            verseReference = "स्तोत्रसंहिता 4:8",
            verseText = "मी शांतीने निजेन आणि झोपेन; कारण हे प्रभू, केवळ तूच मला सुरक्षित ठेवतोस.",
            reflection = "आजचा दिवस पूर्ण झाला आहे. राहिलेली कामे उद्या होतील. रात्री झोपताना तुमचे सर्व प्रश्न देवाकडे सोपवा आणि शांत झोपा.",
            shortPrayer = "देवा, आज दिवसभर सोबत राहिल्याबद्दल धन्यवाद. माझ्या कुटुंबाला सुखाची आणि शांत झोप दे. आमेन.",
            moodTag = "संध्या शांती"
        )
    )
}
