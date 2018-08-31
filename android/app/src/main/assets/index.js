window.onload = function()
{
    var platform = navigator.platform;
    var version = navigator.appVersion;

    var body = window.document.body;
    
    var version = version.toLowerCase();
    if (version.indexOf("iphone") >= 0)
        body.classList.add("ios");
    else if (version.indexOf("android") >= 0)
        body.classList.add("android");
    else
        body.classList.add("foo");
}
