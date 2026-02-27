package com.example.personalfinanceapp.presentation.learning

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.presentation.components.ScreenHeader
import com.example.personalfinanceapp.presentation.home.HomeViewModel
import kotlinx.coroutines.delay

private val lessons = listOf(
    FinanceLesson(
        id = 1,
        title = "Az 50/30/20 szabály",
        icon = Icons.Default.PieChart,
        color = Color(0xFF6366F1),
        level = LessonLevel.BEGINNER,
        summary = "Az egyik legelterjedtebb személyes költségvetési módszer, amely három egyszerű kategóriára osztja a nettó jövedelmedet.",
        keyPoints = listOf(
            "50% – szükségletek: lakbér/törlesztő, rezsi, élelmiszer, közlekedés",
            "30% – vágyak: étterem, szórakozás, előfizetések, ruha",
            "20% – megtakarítás és adósságtörlesztés: vészalap, befektetés, hitel előtörlesztés",
            "Magyarországon a magas lakhatási költségek miatt sokan 60/20/20 arányban alkalmazzák",
            "A lényeg nem a pontos arány, hanem a tudatos elosztás rendszeres fenntartása"
        ),
        source = "Forrás: Elizabeth Warren – All Your Worth (2005); MNB Pénzügyi Tudatossági Program"
    ),
    FinanceLesson(
        id = 2,
        title = "Vészhelyzeti tartalék",
        icon = Icons.Default.Shield,
        color = Color(0xFF10B981),
        level = LessonLevel.BEGINNER,
        summary = "A vészalap az a pénzügyi biztonsági háló, amely váratlan kiadások esetén megvéd a hitelfelvételtől vagy befektetéseid feltörésétől.",
        keyPoints = listOf(
            "Ajánlott méret: 3–6 havi nettó kiadásnak megfelelő összeg",
            "Tartsd likvid, könnyen hozzáférhető helyen – pl. lekötés nélküli bankszámlán",
            "2024-ben a magyarok átlagosan csupán 1,8 havi kiadásnak megfelelő tartalékkal rendelkeznek",
            "Ne keverd össze a befektetéseiddel – a vészalap nem hozamra optimalizált",
            "Először töltsd fel a vészalapot, csak utána kezdj hosszú távú befektetésbe"
        ),
        source = "Forrás: Magyar Nemzeti Bank – Háztartási megtakarítási felmérés 2024"
    ),
    FinanceLesson(
        id = 3,
        title = "Hogyan működik a kamatos kamat?",
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        color = Color(0xFFF59E0B),
        level = LessonLevel.INTERMEDIATE,
        summary = "A kamatos kamat azt jelenti, hogy a kamat is kamatozik – ez az egyik legerősebb mechanizmus a vagyonépítésben, de adósságnál ellened is dolgozhat.",
        keyPoints = listOf(
            "Képlet: Végső összeg = Tőke × (1 + kamatláb)^évek száma",
            "Példa: 1 000 000 Ft, 7% éves hozam, 20 év → ~3 870 000 Ft (közel négyszeres)",
            "A 72-es szabály: osztd el 72-t az éves kamattal – ennyi év alatt duplázódik a tőkéd",
            "Minél korábban kezdesz, annál kevesebbet kell befektetni ugyanolyan végeredményhez",
            "Hiteleknél fordítva működik – a késői törlesztés exponenciálisan növeli a tartozást"
        ),
        source = "Forrás: ÁKK – Befektetői ismeretterjesztő; Investopedia – Compound Interest"
    ),
    FinanceLesson(
        id = 4,
        title = "TBSZ – Tartós Befektetési Számla",
        icon = Icons.Default.AccountBalance,
        color = Color(0xFF8B5CF6),
        level = LessonLevel.INTERMEDIATE,
        summary = "A TBSZ egy magyar adóoptimalizálási eszköz, amellyel a befektetési hozamok után fizetett adót jelentősen csökkentheted – akár nullára.",
        keyPoints = listOf(
            "Gyűjtési év: az első évben helyezed el a tőkét, utána már nem tölthető fel",
            "3 éves lekötés után: az adó 15%-ról 10%-ra csökken (SZJA + szocho kedvezmény)",
            "5 éves lekötés után: teljes adómentesség – se SZJA, se szocho",
            "Részvények, ETF-ek, kötvények, befektetési alapok egyaránt tarthatók TBSZ-en",
            "Minden évben nyithatsz egy új TBSZ-t – érdemes párhuzamos lekötési ciklusokat futtatni",
            "Korai feltörés esetén az összes adókedvezmény elvész"
        ),
        source = "Forrás: NAV – Tartós befektetési számla tájékoztató; hatályos adójogszabályok 2024"
    ),
    FinanceLesson(
        id = 5,
        title = "Magyar Állampapírok",
        icon = Icons.Default.Euro,
        color = Color(0xFF0EA5E9),
        level = LessonLevel.INTERMEDIATE,
        summary = "A magyar állampapírok az egyik legnépszerűbb és legbiztonságosabb megtakarítási eszközök, amelyek a bankbetéteknél jellemzően magasabb hozamot kínálnak.",
        keyPoints = listOf(
            "FixMÁP: rögzített kamatozású, 1–5 éves futamidő, kiszámítható hozam",
            "PMÁP (Prémium Magyar Állampapír): inflációhoz kötött + prémium, jó inflációvédelem",
            "BMÁP (Bónusz Magyar Állampapír): BUBOR-hoz kötött, változó kamatozású",
            "Vásárolható: Posta, MÁP Pont, ÁKK webshop és egyes bankfiókok útján",
            "TBSZ-en tartva az állampapír hozama is adómentessé válhat 5 év után",
            "Az állam garantálja a visszafizetést – ez magasabb védelmet jelent, mint az OBA-limit"
        ),
        source = "Forrás: ÁKK – allamkincstar.gov.hu; Magyar Állampapír termékleírások 2024"
    ),
    FinanceLesson(
        id = 6,
        title = "Önkéntes Nyugdíjpénztár (ÖNYP)",
        icon = Icons.Default.Weekend,
        color = Color(0xFFEC4899),
        level = LessonLevel.ADVANCED,
        summary = "Az önkéntes nyugdíjpénztár hosszú távú, adókedvezményes megtakarítási forma, amellyel kiegészítheted az állami nyugdíjat.",
        keyPoints = listOf(
            "Évi befizetés 20%-a visszaigényelhető az SZJA-ból (max. 150 000 Ft adójóváírás/év)",
            "10 év tagság után részben hozzáférhetsz az összeghez adómentesen",
            "Befektetési portfóliót választhatsz: konzervatív, kiegyensúlyozott, növekedési",
            "Munkáltatói hozzájárulás is lehetséges – érdemes a munkaadóddal egyeztetni",
            "A pénztárak éves kezelési díja 0,5–1,5% körül mozog – érdemes összehasonlítani",
            "Nyugdíjkorhatár elérése (jelenleg 65 év) után teljes adómentességgel vehető fel"
        ),
        source = "Forrás: MABISZ – Önkéntes pénztári összehasonlító; NAV adójóváírás tájékoztató 2024"
    )
)

private val dailyTips = listOf(
    "A magyarok átlagosan jövedelmük 4,2%-át takarítják meg – az EU-s átlag 13%. Kis lépésekkel te is javíthatsz ezen.",
    "Ha minden hónapban 10 000 Ft-tal többet teszel félre, 10 év múlva – 7%-os hozammal számolva – ~1,7 millió Ft-od lesz.",
    "A TBSZ-en tartott befektetés 5 év után teljesen adómentes. Érdemes minden évben nyitni egyet.",
    "Az infláció csökkenti a pénzed értékét: 5%-os inflációnál 14 év alatt feleződik a vásárlóereje.",
    "A legjobb befektetési idő 20 évvel ezelőtt volt. A második legjobb: ma."
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun LearningScreen(viewModel: HomeViewModel) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    val tipIndex = remember { dailyTips.indices.random() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically()
        ) {
            ScreenHeader(
                title = "Tudástár",
                subtitle = "Fejleszd a pénzügyi tudatosságod"
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600, delayMillis = 100)) +
                            slideInVertically(tween(600, delayMillis = 100))
                ) {
                    DailyTipCard(tip = dailyTips[tipIndex])
                }
            }

            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600, delayMillis = 200)) +
                            slideInVertically(tween(600, delayMillis = 200))
                ) {
                    AILearningCard(viewModel)
                }
            }

            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600, delayMillis = 250))
                ) {
                    Text(
                        text = "Tananyagok",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
            }

            itemsIndexed(lessons) { index, lesson ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600, delayMillis = 300 + index * 80)) +
                            slideInVertically(tween(600, delayMillis = 300 + index * 80))
                ) {
                    ExpandableLessonCard(lesson)
                }
            }
        }
    }
}

// ─── Daily tip card ───────────────────────────────────────────────────────────

@Composable
fun DailyTipCard(tip: String) {
    val amber = Color(0xFFF59E0B)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp),
                spotColor = amber.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to amber.copy(alpha = 0.18f),
                            0.6f to amber.copy(alpha = 0.06f),
                            1.0f to Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(amber.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = amber,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tudtad?",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = amber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

// ─── Expandable lesson card ───────────────────────────────────────────────────

@Composable
fun ExpandableLessonCard(lesson: FinanceLesson) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(lesson.color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        lesson.icon,
                        contentDescription = null,
                        tint = lesson.color,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = lesson.level.color.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = lesson.level.label,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = lesson.level.color
                            )
                        }
                        Text(
                            text = "• ${lesson.keyPoints.size} pont",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary
                    Text(
                        text = lesson.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Főbb pontok",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = lesson.color
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    lesson.keyPoints.forEach { point ->
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .background(lesson.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = lesson.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}