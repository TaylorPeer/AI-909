var CHANNEL_NAMES = {1: "KICK", 2: "SNARE", 3: "HIHAT"};

var Sequencer = {};
Sequencer.step = 0;
Sequencer.playing = false;
Sequencer.bpm = 175;
Sequencer.channels = {};
Sequencer.channels.KICK = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
Sequencer.channels.SNARE = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
Sequencer.channels.HIHAT = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
Sequencer.sequenceString = "0000000000000000";
Sequencer.bank = 1;

var waitingForResponse = false;

$(document).ready(function () {

    loadSounds();

    // Add listeners to all step buttons
    $(".drum-button").not(".drum-label").click(function () {

        if (!waitingForResponse) {

            var channelIndex = $(this).parent().parent().parent().index() + 1;
            var channelName = CHANNEL_NAMES[channelIndex];
            var step = $(this).parent().parent().index() - 1;

            if ($(this).hasClass("active")) {
                $(this).removeClass("active");
                Sequencer.channels[channelName][step] = 0;
            } else {
                $(this).addClass("active");
                Sequencer.channels[channelName][step] = 1;
            }
        }
    });

    $("#kick").click(function () {
        createjs.Sound.play("kick");
    });

    $("#snare").click(function () {
        createjs.Sound.play("snare");
    });

    $("#hihat").click(function () {
        createjs.Sound.play("hihat");
    });

    $("#play").click(function () {
        Sequencer.playing = !Sequencer.playing;
        var $playIcon = $("#play-icon");
        if (Sequencer.playing) {
            play();
            $playIcon.removeClass("glyphicon-play");
            $playIcon.addClass("glyphicon-pause");
        } else {
            $playIcon.removeClass("glyphicon-pause");
            $playIcon.addClass("glyphicon-play");
        }
    });

    $("#stop").click(function () {
        Sequencer.playing = false;
        Sequencer.step = 0;
        $(".step-indicator").fadeOut(50);
        var $playIcon = $("#play-icon");
        $playIcon.removeClass("glyphicon-pause");
        $playIcon.addClass("glyphicon-play");
    });

    $("#generate").click(function () {
        if (!waitingForResponse) {
            waitingForResponse = true;
            blink();
            $.get("/getSequence?bpm=" + Sequencer.bpm + "&memoryBank=" + Sequencer.bank, function (data) {
                setSequence(data.sequence);
                waitingForResponse = false;
            });
        }
    });

    $("#mutate").click(function () {
        if (!waitingForResponse) {
            waitingForResponse = true;
            blink();
            $.get("/getSequence?seedSequence=" + getSeedString() + "&bpm=" + Sequencer.bpm + "&memoryBank=" + Sequencer.bank, function (data) {
                setSequence(data.sequence);
                waitingForResponse = false;
            });
        }
    });

    $("#learn").click(function () {
        $.get("/learn?sequence=" + getSeedString() + "&bpm=" + Sequencer.bpm + "&memoryBank=" + Sequencer.bank);
    });

    $("#lower-bpm").click(function () {
        Sequencer.bpm = Math.max(Sequencer.bpm - 1, 1);
        updateBpmDisplay();
    });

    $("#increase-bpm").click(function () {
        Sequencer.bpm = Math.min(Sequencer.bpm + 1, 999);
        updateBpmDisplay();
    });

    $("#lower-bank").click(function () {
        Sequencer.bank = Math.max(Sequencer.bank - 1, 1);
        updateBankDisplay();
    });

    $("#increase-bank").click(function () {
        Sequencer.bank = Math.min(Sequencer.bank + 1, 99);
        updateBankDisplay();
    });
});

function loadSounds() {
    createjs.Sound.registerSound("audio/kick 01.wav", "kick");
    createjs.Sound.registerSound("audio/snare 14.wav", "snare");
    createjs.Sound.registerSound("audio/hh 03.wav", "hihat");
}

function play() {

    if (Sequencer.channels.KICK[Sequencer.step]) {
        createjs.Sound.play("kick");
    }
    if (Sequencer.channels.SNARE[Sequencer.step]) {
        createjs.Sound.play("snare");
    }
    if (Sequencer.channels.HIHAT[Sequencer.step]) {
        createjs.Sound.play("hihat");
    }

    var stepIndicator = $(".step-indicator");
    $(stepIndicator[Sequencer.step]).fadeIn("fast");
    var previousStep = Sequencer.step - 1;
    if (previousStep < 0) {
        previousStep = 15;
    }
    $(stepIndicator[previousStep]).fadeOut(50);


    Sequencer.step = Sequencer.step + 1;
    if (Sequencer.step > 15) {
        Sequencer.step = 0;
    }

    var delay = 60000 / Sequencer.bpm / 2;
    setTimeout(function () {
        if (Sequencer.playing) {
            play();
        }
    }, delay)
}

function updateButtons() {
    var buttons = $(".drum-button").not(".drum-label");
    for (var i = 0; i < 16; i++) {

        // Kick
        if (Sequencer.channels.KICK[i]) {
            $(buttons[i]).addClass("active");
        } else {
            $(buttons[i]).removeClass("active");
        }

        // Snare
        if (Sequencer.channels.SNARE[i]) {
            $(buttons[i + 16]).addClass("active");
        } else {
            $(buttons[i + 16]).removeClass("active");
        }

        // Snare
        if (Sequencer.channels.HIHAT[i]) {
            $(buttons[i + 32]).addClass("active");
        } else {
            $(buttons[i + 32]).removeClass("active");
        }
    }
}

function updateBpmDisplay() {
    var text = Sequencer.bpm;
    if (text < 10) {
        text = "00" + text;
    } else if (text < 100) {
        text = "0" + text;
    }
    $("#bpm").text(text);
}

function updateBankDisplay() {
    var text = Sequencer.bank;
    if (text < 10) {
        text = "0" + text;
    }
    $("#bank").text(text);
}

function getSeedString() {
    var seedString = "";
    for (var i = 0; i < Sequencer.channels.KICK.length; i++) {
        seedString += getEncoding(Sequencer.channels.KICK[i], Sequencer.channels.SNARE[i], Sequencer.channels.HIHAT[i]);
    }
    return seedString;
}

function getEncoding(kick, snare, hihat) {

    if (kick == 1 && snare == 1 && hihat == 1) {
        return "7";
    } else if (kick == 1 && snare == 1 && hihat == 0) {
        return "6";
    } else if (kick == 1 && snare == 0 && hihat == 1) {
        return "5";
    } else if (kick == 0 && snare == 1 && hihat == 1) {
        return "4";
    } else if (kick == 1 && snare == 0 && hihat == 0) {
        return "3";
    } else if (kick == 0 && snare == 1 && hihat == 0) {
        return "2";
    } else if (kick == 0 && snare == 0 && hihat == 1) {
        return "1";
    } else if (kick == 0 && snare == 0 && hihat == 0) {
        return "0";
    }

}

function setSequence(sequence) {
    for (var i = 0; i < sequence.length; i++) {

        var value = sequence.charAt(i);
        switch (value) {
            case "0":
                Sequencer.channels.KICK[i] = 0;
                Sequencer.channels.SNARE[i] = 0;
                Sequencer.channels.HIHAT[i] = 0;
                break;
            case "1":
                Sequencer.channels.KICK[i] = 0;
                Sequencer.channels.SNARE[i] = 0;
                Sequencer.channels.HIHAT[i] = 1;
                break;
            case "2":
                Sequencer.channels.KICK[i] = 0;
                Sequencer.channels.SNARE[i] = 1;
                Sequencer.channels.HIHAT[i] = 0;
                break;
            case "3":
                Sequencer.channels.KICK[i] = 1;
                Sequencer.channels.SNARE[i] = 0;
                Sequencer.channels.HIHAT[i] = 0;
                break;
            case "4":
                Sequencer.channels.KICK[i] = 0;
                Sequencer.channels.SNARE[i] = 1;
                Sequencer.channels.HIHAT[i] = 1;
                break;
            case "5":
                Sequencer.channels.KICK[i] = 1;
                Sequencer.channels.SNARE[i] = 0;
                Sequencer.channels.HIHAT[i] = 1;
                break;
            case "6":
                Sequencer.channels.KICK[i] = 1;
                Sequencer.channels.SNARE[i] = 1;
                Sequencer.channels.HIHAT[i] = 0;
                break;
            case "7":
                Sequencer.channels.KICK[i] = 1;
                Sequencer.channels.SNARE[i] = 1;
                Sequencer.channels.HIHAT[i] = 1;
                break;
            default:
                break;
        }
    }

    updateButtons();
}

function blink() {
    var delay = 200;

    $(".drum-button.active").removeClass("active");

    setTimeout(function () {
        setSequence(getSeedString());
        setTimeout(function () {
            if (waitingForResponse) {
                blink();
            }
        }, delay);
    }, delay);

}

if (!window.console) console = {
    log: function () {
    }
};

var lowLag = new function () {
    this.someVariable = undefined;
    this.showNeedInit = function () {
        lowLag.msg("lowLag: you must call lowLag.init() first!");
    }

    this.load = this.showNeedInit;
    this.play = this.showNeedInit;

    this.useSuspension = false;
    this.suspendDelay = 10000; // ten seconds
    this.suspendTimeout = null;
    this.suspended = false;

    this.audioTagTimeToLive = 5000;

    this.sm2url = 'sm2/swf/';

    this.soundUrl = "";

    this.debug = "console";

    this.divLowLag = null;
    this.divDebug = null;

    this.createElement = function (elemType, attribs) {
        var elem = document.createElement(elemType);
        if (attribs) {
            for (var key in attribs) {
                elem.setAttribute(key, attribs[key]);
            }
        }
        return elem;
    };
    this.safelyRemoveElement = function (elem) {
        if (elem) elem.parentNode.removeChild(elem);
    };
    this.safelyRemoveElementById = function (id) {
        this.safelyRemoveElement(document.getElementById(id));
    };

    this.ready = function ready(fn) {
        if (document.readyState != 'loading') {
            fn();
        } else if (document.addEventListener) {
            document.addEventListener('DOMContentLoaded', fn);
        } else {
            document.attachEvent('onreadystatechange', function () {
                if (document.readyState != 'loading')
                    fn();
            });
        }
    };

    this.init = function (config) {
        //var divLowLag = document.getElementById("lowLag");
        this.safelyRemoveElement(this.divLowLag);
        this.divLowLag = this.createElement("div", {"id": "lowLag"});
        document.body.appendChild(this.divLowLag);

        var force = undefined;
        if (config != undefined) {
            if (config['force'] != undefined) {
                force = config['force'];
            }
            if (config['audioTagTimeToLive'] != undefined) {
                lowLag.audioTagTimeToLive = config['audioTagTimeToLive'];
            }
            if (config['sm2url'] != undefined) {
                lowLag.sm2url = config['sm2url'];
            }
            if (config['urlPrefix'] != undefined) {
                lowLag.soundUrl = config['urlPrefix'];
            }
            if (config['debug'] != undefined) {
                lowLag.debug = config['debug'];
            }
            if (config['useSuspension'] != undefined) {
                lowLag.useSuspension = config['useSuspension'];
            }
            if (config['suspendDelay'] != undefined) {
                lowLag.suspendDelay = config['suspendDelay'];
            }
        }

        if (lowLag.debug == "screen" || lowLag.debug == "both") {
            lowLag.divDebug = lowLag.createElement("pre");
            lowLag.divLowLag.appendChild(lowLag.divDebug);

        }

        var format = "sm2";
        if (force != undefined) format = force; else {
            if (typeof(webkitAudioContext) != "undefined") format = 'webkitAudio'; else if (navigator.userAgent.indexOf("Firefox") != -1) format = 'audioTag';
        }
        switch (format) {
            case 'webkitAudio':

                this.msg("init webkitAudio");
                this.load = this.loadSoundWebkitAudio;
                this.play = this.playSoundWebkitAudio;
                this.webkitAudioContext = new webkitAudioContext();
                if (this.useSuspension &= ('suspend' in lowLag.webkitAudioContext && 'onended' in lowLag.webkitAudioContext.createBufferSource())) {
                    this.playingQueue = [];
                    this.suspendPlaybackWebkitAudio();
                }
                break;
            case 'audioTag':
                this.msg("init audioTag");
                this.load = this.loadSoundAudioTag;
                this.play = this.playSoundAudioTag;
                break;

            case 'sm2':
                this.msg("init SoundManager2");

                this.load = this.loadSoundSM2;
                this.play = this.playSoundSM2;
                lowLag.msg("loading SM2 from " + lowLag.sm2url);
                soundManager.setup({
                    url: lowLag.sm2url, useHighPerformance: true, onready: lowLag.sm2Ready, debugMode: true
                })

                break;

        }

    }
    this.sm2IsReady = false;
    //sm2 has a callback that tells us when it's ready, so we may need to store
    //requests to loadsound, and then call sm2 once it has told us it is set.
    this.sm2ToLoad = [];

    this.loadSoundSM2 = function (url, tag) {
        if (lowLag.sm2IsReady) {
            lowLag.loadSoundSM2ForReals(url, tag);
        } else {
            lowLag.sm2ToLoad.push([url, tag]);
        }
    }

    this.loadSoundSM2ForReals = function (urls, ptag) {
        var tag = lowLag.getTagFromURL(urls, ptag);
        lowLag.msg('sm2 loading ' + urls + ' as tag ' + tag);
        var urls = lowLag.getURLArray(urls); //coerce
        for (var i = 0; i < urls.length; i++) {
            var url = lowLag.soundUrl + urls[i];
            urls[i] = url;
        }

        soundManager.createSound({
            id: tag, autoLoad: true, url: urls
        });
    };

    this.sm2Ready = function () {
        lowLag.sm2IsReady = true;
        for (var i = 0; i < lowLag.sm2ToLoad.length; i++) {
            var urlAndTag = lowLag.sm2ToLoad[i];
            lowLag.loadSoundSM2ForReals(urlAndTag[0], urlAndTag[1]);
        }
        lowLag.sm2ToLoad = [];
    }

    this.playSoundSM2 = function (tag) {
        lowLag.msg("playSoundSM2 " + tag);

        soundManager.play(tag);
    }

    //we'll use the tag they hand us, or else the url as the tag if it's a single tag,
    //or the first url
    this.getTagFromURL = function (url, tag) {
        if (tag != undefined) return tag;
        return lowLag.getSingleURL(url);
    }
    this.getSingleURL = function (urls) {
        if (typeof(urls) == "string") return urls;
        return urls[0];
    }
    //coerce to be an array
    this.getURLArray = function (urls) {
        if (typeof(urls) == "string") return [urls];
        return urls;
    }

    this.webkitPendingRequest = {};

    this.webkitAudioContext = undefined;
    this.webkitAudioBuffers = {};

    this.loadSoundWebkitAudio = function (urls, tag) {
        var url = lowLag.getSingleURL(urls);
        var tag = lowLag.getTagFromURL(urls, tag);
        lowLag.msg('webkitAudio loading ' + url + ' as tag ' + tag);
        var request = new XMLHttpRequest();
        request.open('GET', lowLag.soundUrl + url, true);
        request.responseType = 'arraybuffer';

        // Decode asynchronously
        request.onload = function () {
            lowLag.webkitAudioContext.decodeAudioData(request.response, function (buffer) {
                lowLag.webkitAudioBuffers[tag] = buffer;

                if (lowLag.webkitPendingRequest[tag]) { //a request might have come in, try playing it now
                    lowLag.playSoundWebkitAudio(tag);
                }
            }, lowLag.errorLoadWebkitAudtioFile);
        };
        request.send();
    }

    this.errorLoadWebkitAudtioFile = function (e) {
        lowLag.msg("Error loading webkitAudio: " + e);
    }

    this.playSoundWebkitAudio = function (tag) {
        lowLag.msg("playSoundWebkitAudio " + tag);
        var buffer = lowLag.webkitAudioBuffers[tag];
        if (buffer == undefined) { //possibly not loaded; put in a request to play onload
            lowLag.webkitPendingRequest[tag] = true;
            return;
        }
        var context = lowLag.webkitAudioContext;
        if (this.useSuspension && this.suspended) {
            this.resumePlaybackWebkitAudio(); // Resume playback
        }
        var source = context.createBufferSource(); // creates a sound source
        source.buffer = buffer;                    // tell the source which sound to play
        source.connect(context.destination);       // connect the source to the context's destination (the speakers)
        if (typeof(source.noteOn) == "function") {
            source.noteOn(0);                          // play the source now, using noteOn
        } else {
            if (this.useSuspension) {
                this.playingQueue.push(tag);
                source.onended = function (e) {
                    lowLag.hndlOnEndedWebkitAudio(tag, e);
                }
            }
            source.start();				// play the source now, using start
        }
    }

    this.hndlOnEndedWebkitAudio = function (tag, e) {
        for (var i = 0; i < this.playingQueue.length; i++) {
            if (this.playingQueue[i] == tag) {
                this.playingQueue.splice(i, 1);
                break;
            }
        }
        if (!this.playingQueue.length) {
            this.suspendPlaybackWebkitAudio();
        }
    }

    this.resumePlaybackWebkitAudio = function () {
        this.webkitAudioContext.resume();
        this.suspended = false;
    }

    this.suspendPlaybackWebkitAudio = function () {
        if (this.suspendTimeout) {
            clearTimeout(this.suspendTimeout);
        }
        this.suspendTimeout = setTimeout(function () {
            lowLag.webkitAudioContext.suspend();
            lowLag.suspended = true;
            lowLag.suspendTimeout = null;
        }, this.suspendDelay);
    }

    this.audioTagID = 0;
    this.audioTagNameToElement = {};

    this.loadSoundAudioTag = function (urls, tag) {
        var id = "lowLagElem_" + lowLag.audioTagID++;

        var tag = lowLag.getTagFromURL(urls, tag);

        var urls = lowLag.getURLArray(urls);

        lowLag.audioTagNameToElement[tag] = id;

        lowLag.msg('audioTag loading ' + urls + ' as tag ' + tag);
        var audioElem = this.createElement("audio", {"id": id, "preload": "auto", "autobuffer": "autobuffer"})

        for (var i = 0; i < urls.length; i++) {
            var url = urls[i];
            var type = "audio/" + lowLag.getExtension(url);
            var sourceElem = this.createElement("source", {"src": lowLag.soundUrl + url, "type": type});
            audioElem.appendChild(sourceElem);
        }

        document.body.appendChild(audioElem);
    }

    this.playSoundAudioTag = function (tag) {
        lowLag.msg("playSoundAudioTag " + tag);

        var modelId = lowLag.audioTagNameToElement[tag];
        var cloneId = "lowLagCloneElem_" + lowLag.audioTagID++;

        var modelElem = document.getElementById(modelId);
        var cloneElem = modelElem.cloneNode(true);
        cloneElem.setAttribute("id", cloneId);
        this.divLowLag.appendChild(cloneElem);
        lowLag.msg(tag);
        if (lowLag.audioTagTimeToLive != -1) {
            setTimeout(function () {
                lowLag.safelyRemoveElement(cloneElem);
            }, lowLag.audioTagTimeToLive);
        }
        cloneElem.play();

    }

    this.getExtension = function (url) {
        return url.substring(url.lastIndexOf(".") + 1).toLowerCase();

    }

    this.msg = function (m) {
        m = "-- lowLag " + m;
        if (lowLag.debug == 'both' || lowLag.debug == 'console') {
            console.log(m);
        }
        if (lowLag.divDebug) {
            lowLag.divDebug.innerHTML += m + "\n";
        }
    }

}