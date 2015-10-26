var CHANNEL_NAMES = {1: "KICK", 2: "SNARE", 3: "HIHAT"};

var Sequencer = {};
Sequencer.step = 0;
Sequencer.playing = false;
Sequencer.bpm = 180;
Sequencer.channels = {};
Sequencer.channels.KICK = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
Sequencer.channels.SNARE = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
Sequencer.channels.HIHAT = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
Sequencer.sequenceString = "0000000000000000";

$(document).ready(function () {

    loadSounds();

    // Add listeners to all step buttons
    $(".drum-button").not(".drum-label").click(function () {

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
        if (Sequencer.playing) {
            play();
        }
    });

    $("#generate").click(function () {

        $.get("/getSequence?bpm=180&intensity=0.5", function (data) {
            for (var i = 0; i < data.sequence.length; i++) {

                var value = data.sequence.charAt(i);
                switch(value) {
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

        });
    })
});

function loadSounds() {
    createjs.Sound.registerSound("audio/Kick01.mp3", "kick");
    createjs.Sound.registerSound("audio/Snare01.mp3", "snare");
    createjs.Sound.registerSound("audio/Hat01.mp3", "hihat");
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

    Sequencer.step = Sequencer.step + 1;
    if (Sequencer.step > 15) {
        Sequencer.step = 0;
    }

    setTimeout(function () {
        if (Sequencer.playing) {
            play();
        }
    }, 167)
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