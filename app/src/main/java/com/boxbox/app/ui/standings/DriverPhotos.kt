package com.boxbox.app.ui.standings

/**
 * High-resolution driver photo URLs, scraped directly from formula1.com/en/drivers
 * (Cloudinary CDN, confirmed real by fetching that page's HTML rather than guessed).
 * Unlike OpenF1's headshot_url (an older, fixed-small-size CDN render), these support
 * a "w_" width parameter, so requesting w_640 instead of the page's default w_64
 * returns a sharp, full-resolution version of the same official photo.
 *
 * Used only on the Driver Detail screen's hero image, where a low-res thumbnail is most
 * noticeable; the Standings list intentionally keeps using OpenF1's small image since
 * loading ~20 of these at once would be unnecessary for a list of small avatars.
 *
 * Keyed by normalized (accent-stripped, lowercase) surname.
 */
val driverHighResPhotos: Map<String, String> = mapOf(
    "albon" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/williams/alealb01/2026williamsalealb01right.webp",
    "alonso" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/astonmartin/feralo01/2026astonmartinferalo01right.webp",
    "antonelli" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/mercedes/andant01/2026mercedesandant01right.webp",
    "bearman" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/haasf1team/olibea01/2026haasf1teamolibea01right.webp",
    "bortoleto" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/audi/gabbor01/2026audigabbor01right.webp",
    "bottas" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/cadillac/valbot01/2026cadillacvalbot01right.webp",
    "colapinto" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/alpine/fracol01/2026alpinefracol01right.webp",
    "gasly" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/alpine/piegas01/2026alpinepiegas01right.webp",
    "hadjar" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/redbullracing/isahad01/2026redbullracingisahad01right.webp",
    "hamilton" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/ferrari/lewham01/2026ferrarilewham01right.webp",
    "hulkenberg" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/audi/nichul01/2026audinichul01right.webp",
    "lawson" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/racingbulls/lialaw01/2026racingbullslialaw01right.webp",
    "leclerc" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/ferrari/chalec01/2026ferrarichalec01right.webp",
    "lindblad" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/racingbulls/arvlin01/2026racingbullsarvlin01right.webp",
    "norris" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/mclaren/lannor01/2026mclarenlannor01right.webp",
    "ocon" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/haasf1team/estoco01/2026haasf1teamestoco01right.webp",
    "perez" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/cadillac/serper01/2026cadillacserper01right.webp",
    "piastri" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/mclaren/oscpia01/2026mclarenoscpia01right.webp",
    "russell" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/mercedes/georus01/2026mercedesgeorus01right.webp",
    "sainz" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/williams/carsai01/2026williamscarsai01right.webp",
    "stroll" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/astonmartin/lanstr01/2026astonmartinlanstr01right.webp",
    "verstappen" to "https://media.formula1.com/image/upload/c_lfill,w_640/q_auto/d_common:f1:2026:fallback:driver:2026fallbackdriverright.webp/v1740000001/common/f1/2026/redbullracing/maxver01/2026redbullracingmaxver01right.webp"
)
