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
    createjs.Sound.registerSound("audio/kick 01.mp3", "kick");
    createjs.Sound.registerSound("audio/snare 14.mp3", "snare");
    createjs.Sound.registerSound("audio/hh 03.mp3", "hihat");
}

function play() {

    $($(".step-indicator")[Sequencer.step]).fadeIn("fast");
    var previousStep = Sequencer.step - 1;
    if (previousStep < 0) {
        previousStep = 15;
    }
    $($(".step-indicator")[previousStep]).fadeOut(50);

    if (Sequencer.channels.KICK[Sequencer.step]) {
        createjs.Sound.play("kick");
    }
    if (Sequencer.channels.SNARE[Sequencer.step]) {
        createjs.Sound.play("snare");
    }
    if (Sequencer.channels.HIHAT[Sequencer.step]) {
        createjs.Sound.play("hihat");
    }

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