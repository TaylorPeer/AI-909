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

var updateSteps = function () {
};

var waitingForResponse = false;
var interactionEvent = "click";

function rebindMobileEvents() {

    var $kick = $("#kick");
    $kick.unbind("click");
    $kick.bind(interactionEvent, function () {
        createjs.Sound.play("kick");
    });

    var $snare = $("#snare");
    $snare.unbind("click");
    $snare.bind(interactionEvent, function () {
        createjs.Sound.play("snare");
    });

    var $hihat = $("#hihat");
    $hihat.unbind("click");
    $hihat.bind(interactionEvent, function () {
        createjs.Sound.play("hihat");
    });

    var $play = $("#play");
    $play.unbind("click");
    $play.bind(interactionEvent, function () {
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
}

$(document).ready(function () {

    // Device detection
    var isMobile = false;
    if (/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|ipad|iris|kindle|Android|Silk|lge |maemo|midp|mmp|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i.test(navigator.userAgent) || /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(navigator.userAgent.substr(0, 4))) isMobile = true;

    if (isMobile) {
        interactionEvent = "touchstart";
        $("#app").css({position: "relative", top: "50%", transform: "translateY(-50%)"});
        var height = $(window).height();
        $("body").height(height + 80);
        window.scrollTo(0, document.body.scrollHeight);
    }

    loadSounds();

    // Add listeners to all step buttons
    $(".drum-button").not(".drum-label").bind(interactionEvent, function () {

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

    $("#kick").bind("click", function () {
        createjs.Sound.play("kick");
        if (interactionEvent != "click") {
            rebindMobileEvents();
        }
    });

    $("#snare").bind("click", function () {
        createjs.Sound.play("snare");
        if (interactionEvent != "click") {
            rebindMobileEvents();
        }
    });

    $("#hihat").bind("click", function () {
        createjs.Sound.play("hihat");
        if (interactionEvent != "click") {
            rebindMobileEvents();
        }
    });

    $("#play").bind("click", function () {
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
        if (interactionEvent != "click") {
            rebindMobileEvents();
        }
    });

    $("#stop").bind(interactionEvent, function () {
        Sequencer.playing = false;
        Sequencer.step = 0;
        $(".step-indicator").fadeOut(50);
        var $playIcon = $("#play-icon");
        $playIcon.removeClass("glyphicon-pause");
        $playIcon.addClass("glyphicon-play");
        updateSteps();
        updateSteps = function () {
        };
    });

    $("#generate").bind(interactionEvent, function () {
        if (!waitingForResponse) {
            waitingForResponse = true;
            blink();
            $.get("/getSequence?bpm=" + Sequencer.bpm + "&memoryBank=" + Sequencer.bank, function (data) {
                if (Sequencer.playing) {
                    updateSteps = function () {
                        setSequence(data.sequence);
                        waitingForResponse = false;
                    };
                } else {
                    setSequence(data.sequence);
                    waitingForResponse = false;
                }
            });
        }
    });

    $("#mutate").bind(interactionEvent, function () {
        if (!waitingForResponse) {
            waitingForResponse = true;
            blink();
            $.get("/getSequence?seedSequence=" + getSeedString() + "&bpm=" + Sequencer.bpm + "&memoryBank=" + Sequencer.bank, function (data) {
                if (Sequencer.playing) {
                    updateSteps = function () {
                        setSequence(data.sequence);
                        waitingForResponse = false;
                    };
                } else {
                    setSequence(data.sequence);
                    waitingForResponse = false;
                }
            });
        }
    });

    $("#learn").bind(interactionEvent, function () {
        $.get("/learn?sequence=" + getSeedString() + "&bpm=" + Sequencer.bpm + "&memoryBank=" + Sequencer.bank);
    });

    $("#lower-bpm").bind(interactionEvent, function () {
        Sequencer.bpm = Math.max(Sequencer.bpm - 1, 1);
        updateBpmDisplay();
    });

    $("#increase-bpm").bind(interactionEvent, function () {
        Sequencer.bpm = Math.min(Sequencer.bpm + 1, 999);
        updateBpmDisplay();
    });

    $("#lower-bank").bind(interactionEvent, function () {
        Sequencer.bank = Math.max(Sequencer.bank - 1, 1);
        updateBankDisplay();
    });

    $("#increase-bank").bind(interactionEvent, function () {
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
    $(stepIndicator[Sequencer.step]).show();
    var previousStep = Sequencer.step - 1;
    if (previousStep < 0) {
        previousStep = 15;
    }
    $(stepIndicator[previousStep]).hide();

    Sequencer.step = Sequencer.step + 1;
    if (Sequencer.step > 15) {
        updateSteps();
        updateSteps = function () {
        };
        Sequencer.step = 0;
    }

    var delay = 60000 / Sequencer.bpm / 2 / 2;
    setTimeout(function () {
        if (Sequencer.playing) {
            play();
        }
    }, delay)
}

function updateButtons() {
    $(".drum-button.active").removeClass("blink");
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
    $(".drum-button.active").addClass("blink");
}