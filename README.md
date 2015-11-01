# AI-909
Intelligent Drum Machine powered by Numenta's Hierarchical Temporal Memory. Created for the [HTM Challenge 2015](http://htmchallenge.devpost.com/)

## Requirements
- Java 8
- Maven

## Compatibility
- Working on: Chrome, Safari, iOS
- Known issues: Firefox, Android (maybe one day I will have the courage to open IE and test it there as well)

## Instructions
- **git clone** and cd to the top of the project directory
- **mvn spring-boot:run**
- Open **http://localhost:8080/** in a compatible browser 
- Alternatively: use your machine's local IP to run the frontend on a mobile device over a local network)

## Notes
- Memory bank 1 comes preloaded with breakbeat training sequences!
- Any sequences stored by pressing the "learn" button get added to src/main/resources/training-sequences.tsv. There isn't a way to delete learned sequences from the UI yet so you'll have to manually remove the string representation of learned patterns there for the moment.
- Sometimes the backend gets stuck in an endless loop when trying to validate the generated sequences, need to fix this.
- Haven't really tested anything other than breakbeats yet...
