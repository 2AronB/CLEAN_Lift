package dev.clean.lift.data

data class ResearchLink(val title: String, val url: String)
data class HealthGuide(val requirements: String, val protocol: String, val formula: String, val interpretation: String, val sources: List<ResearchLink>)

object HealthGuides {
    private val cdc = ResearchLink("CDC • adult BMI categories", "https://www.cdc.gov/bmi/adult-calculator/bmi-categories.html")
    private val nice = ResearchLink("NICE • waist-to-height guidance", "https://www.nice.org.uk/guidance/ng246/chapter/Identifying-and-assessing-overweight-obesity-and-central-adiposity")
    private val who = ResearchLink("WHO • waist and waist-to-hip report", "https://www.who.int/publications/i/item/9789241501491")
    private val tape = ResearchLink("Tape equations, protocol and change validation • 2023", "https://www.frontiersin.org/journals/physiology/articles/10.3389/fphys.2023.1183836/full")
    private val rfm = ResearchLink("RFM equation development • 2018", "https://www.nature.com/articles/s41598-018-29362-1")
    private val rfmOutcome = ResearchLink("RFM cutoffs and mortality association • 2020", "https://pubmed.ncbi.nlm.nih.gov/31911664/")
    private val fatOutcome = ResearchLink("Body-fat distribution and mortality • NHANES cohort", "https://pubmed.ncbi.nlm.nih.gov/29474498/")
    private val friend = ResearchLink("FRIEND • measured treadmill VO₂ percentiles, Table 3", "https://pmc.ncbi.nlm.nih.gov/articles/PMC4919021/")
    private val cardio = ResearchLink("AHA • cardiorespiratory fitness and health outcomes", "https://www.ahajournals.org/doi/10.1161/CIR.0000000000000461")
    private val tapeBasics = "Use a non-stretch tape, keep it flat and snug without compressing skin, and repeat measurements to check consistency. Record measurements from the same session."
    fun forKind(kind: HealthKind, gender: String): HealthGuide = when (kind) {
        HealthKind.BMI -> HealthGuide("Scale and height measurement; age 20+ for CDC adult categories.",
            "Measure height without shoes. Weigh under similar conditions. Height and weight are saved with each BMI result.",
            "BMI = weight (kg) ÷ height (m)².",
            "CDC categories are weight-status screening labels, not diagnoses. Muscle mass and body-fat distribution are not separated by BMI. Adult thresholds are not children's BMI percentiles.", listOf(cdc))
        HealthKind.WAIST_HEIGHT -> HealthGuide("Non-stretch tape and height; optional weight to check NICE's BMI <35 scope; age 18+.",
            "$tapeBasics For NICE waist measurement, use the midpoint between the lowest rib and top of the hip bones after breathing out normally.",
            "Waist-to-height = waist ÷ height, using the same units.",
            "NICE's adult ranges describe central adiposity and associated diabetes, hypertension and cardiovascular risk. Their stated scope is BMI below 35. A ratio below 0.4 is outside the published healthy band, not an extra reward.", listOf(nice))
        HealthKind.WAIST_HIP -> HealthGuide("Non-stretch tape; waist and hip measurements; profile gender and adult age for reference labels.",
            "$tapeBasics Use waist midway between the lowest rib and hip crest, and hips at the widest buttock circumference.",
            "Waist-to-hip = waist ÷ hips, using the same units.",
            "WHO discusses increased-risk adult thresholds M ≥0.90 and F ≥0.85. Age, ethnicity and technique affect interpretation. These are population screening references.", listOf(who))
        HealthKind.NAVY_FAT -> HealthGuide("Adult circumference estimate (18+). Non-stretch tape, height and profile gender. M: neck and abdomen. F: neck, natural waist and hips. Optional same-session weight for estimated fat/fat-free mass.",
            "$tapeBasics Neck: just below the larynx, shoulders relaxed. " + if (gender == "F") "Natural waist: narrowest torso point; hips: widest buttocks." else "Abdomen: at the navel after normal exhalation. The natural waist and clinical midpoint waist are different sites.",
            "Hodgdon–Beckett circumference equation (historical Navy method). Inches: M = 86.010 log10(abdomen−neck) −70.041 log10(height) +36.76; F = 163.205 log10(waist+hips−neck) −97.684 log10(height) −78.387. Centimetres are converted to inches first.",
            "An accessible estimate with population-dependent individual error. Consistent technique helps repeatability; a small numerical change does not necessarily establish a change in body fat. This historical equation is not a current Navy pass/fail assessment.", listOf(tape, ResearchLink("Circumference equation validation • Marine Corps", "https://www.frontiersin.org/journals/physiology/articles/10.3389/fphys.2022.868627/full"), fatOutcome))
        HealthKind.RFM_FAT -> HealthGuide("Non-stretch tape, height and profile gender; adults 20+ for the outcome reference. Optional same-session weight.",
            "$tapeBasics RFM was developed from NHANES measurements: waist at the upper border of the right iliac crest (top of hip bone), tape horizontal, after normal exhalation. This differs from NICE's midpoint waist and the Navy natural waist.",
            "RFM = 64 −20 × height/waist +12 if profile gender F. Height and waist use the same units.",
            "A separate body-fat estimate, not interchangeable with Navy or DXA. An NHANES mortality study proposed RFM cutoffs of 30% for men and 40% for women. Association does not specify an individual's risk or prove a target is optimal.", listOf(rfm, rfmOutcome))
        HealthKind.ROCKPORT -> HealthGuide("Measured level outdoor track/course of exactly 1 mile (1,609.344 m), stopwatch, scale, and finishing pulse in bpm (heart-rate monitor or careful manual measurement). Profile age 20–69 and gender required.",
            "Warm up, then walk one mile as quickly as possible without jogging. Record elapsed minutes:seconds and heart rate immediately at completion. Use the same course and pulse method when comparing tests. This equation is for a track walk, not treadmill walking. Stop if you develop chest pain, dizziness or unusual breathlessness.",
            "Kline/Rockport: 132.853 −0.0769 × weight(lb) −0.3877 × age +6.315 if M −3.2649 × minutes −0.1565 × finishing bpm.",
            "Estimated VO₂max. Heart-rate-altering medication, weather and measurement delays affect results. FRIEND comparisons are approximate: its reference values came from measured maximal treadmill tests.", listOf(ResearchLink("Kline • original one-mile walk validation", "https://pubmed.ncbi.nlm.nih.gov/3600239/"), ResearchLink("Treadmill walk requires a different model", "https://pubmed.ncbi.nlm.nih.gov/12500996/"), friend, cardio))
        HealthKind.COOPER -> HealthGuide("Adult field estimate (18+). Measured level running track/course, stopwatch or external 12-minute timer, and total distance in metres. No pulse monitor required. Age/gender are optional for comparison context.",
            "After warming up, cover as much distance as possible in exactly 12 minutes. Record completed laps plus the partial lap distance. Use the same surface and similar conditions. This is a strenuous test originally studied in male military personnel; population and pacing affect the estimate. Stop for chest pain, dizziness or unusual breathlessness.",
            "Estimated VO₂max = (distance in metres −504.9) ÷44.73. Save the raw distance too.",
            "A field estimate, not a laboratory measurement. FRIEND treadmill percentiles provide approximate comparison context, not a validated field-test health category.", listOf(ResearchLink("Cooper • original field test paper", "https://pubmed.ncbi.nlm.nih.gov/5694044/"), ResearchLink("Cooper Institute • test background", "https://www.cooperinstitute.org/blog/50-years-of-the-cooper-12-minute-run"), friend, cardio))
        else -> if (kind.bodyFat) HealthGuide("A result from ${kind.title.substringAfter("• ").lowercase()}; record device/protocol in notes. Optional same-session weight, age and profile gender.",
            "Enter the reported percentage. Keep each method separate and use the same device/protocol for comparisons. DXA requires a scanner; impedance a compatible scale/device; skinfold estimates calipers and a defined equation.",
            "No body-fat equation applied to a manually entered result. Fat mass = weight × body-fat%/100; fat-free mass = weight − fat mass (not muscle mass).",
            "Studies connect body-fat amount and distribution to health outcomes, but their ranges differ by population and measurement method. No universal athletic cutoff is assigned.", listOf(fatOutcome))
        else HealthGuide("${if (kind == HealthKind.VO2_LAB) "Laboratory treadmill exercise test with gas-analysis equipment" else "Device estimate or other reported VO₂ result"}, in mL/kg/min. Record device, exercise mode and VO₂max/VO₂peak label in notes. Optional age/gender for reference.",
            "Enter the reported relative oxygen-uptake value, not L/min. Keep laboratory treadmill and device/other results separate. Record cycle tests under device/other because the displayed reference is treadmill-specific.",
            "No prediction formula applied to a manually entered result.",
            "Higher cardiorespiratory fitness is associated with better health outcomes at population level. FRIEND percentiles describe adults without known cardiovascular disease; they are not individual health-risk probabilities.", listOf(friend, cardio))
    }
}
