import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import org.w3c.dom.events.EventListener
import org.w3c.dom.get
import org.w3c.dom.url.URLSearchParams
import kotlin.js.Promise

// variable to tell if the button can be pressed or not
var canClick = true

fun main() = MutationObserver { _, _ ->
    // get necessary elements off the bat
    val menuContainer = document.getElementById("menu-container")
    val downloadButton = document.getElementById("download-button")
    val menu = menuContainer?.children?.get(0)

    // check if a download button should be placed
    if (menu?.children?.get(0) == null) return@MutationObserver
    if (downloadButton != null) return@MutationObserver

    // set the button's color to how it should be based on if the page is in dark mode or not
    setButtonColor()
    // activate listener for the dark mode toggle
    darkObserver.observe(document.getElementsByTagName("html")[0]!!, MutationObserverInit(attributes = true))

    // get the button list and append the download button
    val buttonList = menuContainer.children[0]!!.children[0]!!.children[0]!!
    val element = elementFromHTML(button)
    buttonList.appendChild(element)

    // get the download button and set up the event listener for it
    val dlButton = document.getElementById("download-button")!!
    dlButton.addEventListener("click", onDownloadButton())

}.observe(document.body!!, MutationObserverInit(
    childList = true,
    subtree = true
))

fun onDownloadButton() = EventListener {
    if (canClick) {
        canClick = false

        // get the 'v' url param
        val urlParams = URLSearchParams(window.location.search)
        val youtubeUrl = urlParams.get("v")

        // if the youtube url param 'v' doesn't exist, show an alert
        if (youtubeUrl == null) {
            console.log("Invalid youtube url? No v parameter found.")
            window.alert("'v' parameter not found. This probably means that YouTube has updated its API. Sorry for the inconvenience")
            return@EventListener
        }

        // use an iFrame to start the download. this is better than other methods like changing the href because
        // changing the href would revert to the previous page too quickly, due to how slow the initial request is.
        val rawIFrame = downloadIFrame(youtubeUrl)
        val iFrame = elementFromHTML(rawIFrame)
        document.body!!.appendChild(iFrame)

        // set a cooldown of 30s to prevent spam. people can still theoretically spam the download link
        // but that's a lot less likely for someone to do than just click the button a lot since they think
        // it isn't working.
        window.setTimeout(handler = {
            canClick = true
        }, timeout = 30000)
    } else {
        window.alert("You can only click this once every 30 seconds. If the download hasn't started yet, be patient. It takes some time to process the video.")
    }
}

fun waitForElement(id: String) : Promise<Element> {
    return Promise { resolve, _ ->
        // check if the element already exists, if so then just return it.
        val possibleElement = document.getElementById(id)
        if (possibleElement != null) {
            resolve(possibleElement)
            return@Promise
        }
        // set up a mutation observer to check when anything is modified if it was that element, then return it.
        val observer = MutationObserver { _, self ->
            val element = document.getElementById(id)
            if (element != null) {
                resolve(element)
                self.disconnect()
            }
        }

        // activate observer
        observer.observe(document, MutationObserverInit(
            childList = true,
            subtree = true
        ))
    }
}

val button = """
    <a style="display: block;transform: translateY(6px) translateX(6px);" class="yt-simple-endpoint style-scope ytd-button-renderer" tabindex="-1" id="download-button">
        <svg xmlns="http://www.w3.org/2000/svg" id="download-svg" enable-background="new 0 0 24 24" height="24px" viewBox="0 0 24 24" width="24px" fill="#FFFFFF">
            <g class="style-scope yt-icon">
                <rect fill="none" height="24" width="24"/>
            </g>
            <g class="style-scope yt-icon">
                <path d="M5,20h14v-2H5V20z M19,9h-4V3H9v6H5l7,7L19,9z"/>
            </g>
        </svg>
    </a>
""".trimIndent()

val darkObserver = MutationObserver { _, _ ->
    setButtonColor()
}

fun setButtonColor() {
    // get the current dark mode settings
    val dark = document.getElementsByTagName("html")[0]!!.getAttribute("dark") == "true"
    if (dark) {
        waitForElement("download-svg").then {
            it.setAttribute("fill", "#FFFFFF")
        }
    } else {
        waitForElement("download-svg").then {
            it.setAttribute("fill", "#000000")
        }
    }
}

fun elementFromHTML(html: String) : Element {
    // create a template which the innerHTML can be set to a string, then return the innerHTML as an element.
    val template = document.createElement("div")
    template.innerHTML = html
    return template.firstElementChild!!
}

fun downloadIFrame(url: String) = "<iframe src=\"https://mastriel.xyz/api/downloadVideo?url=https://www.youtube.com/watch?v=$url\" style=\"position: absolute;width:0;height:0;border:0;\"></iframe>"

