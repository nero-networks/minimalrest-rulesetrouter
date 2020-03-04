window.onload = ()=> {
    var id;

    if (!location.search) {
        if (id = prompt("Enter bieg id")) {
            location.assign(location.href+'?'+id)
            return
        }
    }

    var id =  location.search.substr(1),
        ele = (s, p)=> (p||document).querySelector(s)
        Source = (root)=> {
            var textarea = ele("textarea", root), button = ele("button", root),
                msg = (txt)=> JSON.stringify(eval("({"+txt+", date: new Date(), origin: '/"+id+"'})"), null, 4)

            button.onclick = ()=> fetch("/deliver", {method: 'post', body: msg(textarea.value)})
        },
        Route = (root)=> {
            var ul = ele("ul", root), textarea = ele("textarea", root), status = ele("#status")
                updateRules = textarea.onchange = ()=> fetch("/rules/"+id, {method: 'put', body: textarea.value}),
                client = MessageBrokerClient({
                    connected: ()=> client.send("SUBSCRIBE", "/" + id),
                    status: (txt)=> status.textContent = txt,
                    handle: (msg)=> {
                        var li = document.createElement("li")
                        li.innerHTML = '<pre style="font-weight:bold"></pre>'

                        var pre = ele("pre", li)
                        pre.textContent = msg.substr(2, msg.length -3)

                        setTimeout(()=> {
                            pre.removeAttribute("style")
                        }, 5000)

                        if (ul.firstChild) {
                            setTimeout(()=> {
                                ul.removeChild(ul.lastChild)
                            }, (9 + ul.children.length) * 1000)
                        }

                        ul.insertBefore(li, ul.firstChild)
                    }
                })
            setTimeout(()=>
                fetch("/rules/"+id)
                    .then((res)=> res.text())
                    .then((txt)=> textarea.value = txt))

        }

    Source(ele("#source"))
    Route(ele("#route"))
}
