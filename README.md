# AI-909
Intelligent Drum Machine powered by Numenta's Hierarchical Temporal Memory. Created for the [HTM Challenge 2015](http://htmchallenge.devpost.com/).

<img src="https://raw.githubusercontent.com/TaylorPeer/AI-909/master/ai909-ipad.png" height="300px">

Demo video: [https://www.youtube.com/watch?v=2y4549AjgEE](https://www.youtube.com/watch?v=2y4549AjgEE)

## Requirements
- Java 8
- Maven

## Compatibility
- Tested and working on: Chrome (Mac and PC), Safari (Mac), Mobile Safari (iOS 8)
- Known issues:
  - Firefox: display issues, probably due to Webkit-specific CSS rules
  - Android: works in Chrome on Android but the audio timing is way off. This is partially due to Android performance issues but mostly due to me using setTimeout to sync sounds rather than using the WebAudioAPI like I should.

## Instructions
- **git clone** and **cd** to the top of the project directory
- **mvn spring-boot:run**
- Open **http://localhost:8080/** in a compatible browser 
- Alternatively: use your machine's local IP to run the frontend on a mobile device over a local network

## Notes
- Memory bank 1 comes preloaded with breakbeat training sequences!
- Any sequences stored by pressing the "learn" button get added to src/main/resources/training-sequences.tsv. There isn't a way to delete learned sequences from the UI yet so you'll have to manually remove the string representation of learned patterns there for the moment.
- Sometimes the backend gets stuck in an endless loop when trying to validate the generated sequences, need to fix this.
- Haven't really tested anything other than breakbeats yet...
