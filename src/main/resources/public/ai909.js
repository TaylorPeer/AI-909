var Sequencer = {};
Sequencer.bpm = 180;
Sequencer.channels = {};
Sequencer.channels.KICK  = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
Sequencer.channels.SNARE = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
Sequencer.channels.HIHAT = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
Sequencer.sequenceString = "0000000000000000";

$( document ).ready(function() {

    // Add listeners to all step buttons
    $(".drum-button").not(".drum-label").click(function() {
        if ($(this).hasClass("active")) {
            $(this).removeClass("active");
        } else {
            $(this).addClass("active");
        }
    });
});