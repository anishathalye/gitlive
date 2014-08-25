var fakeLocs = [
    {
        origin: {
            latitude: 50,
            longitude: 30
        },
        destination: {
            latitude: 60,
            longitude: 40
        },
        options: {
            color: 'blue'
        }
    },
    {
        origin: {
            latitude: -30,
            longitude: 30
        },
        destination: {
            latitude: 0,
            longitude: -25
        },
        options: {
            color: 'red'
        }
    },
    {
        origin: {
            latitude: -15,
            longitude: -20
        },
        destination: {
            latitude: 20,
            longitude: -5
        },
        options: {
            color: 'green'
        }
    },
    {
        origin: {
            latitude: 60,
            longitude: -120
        },
        destination: {
            latitude: 20,
            longitude: -80
        },
        options: {
            color: 'yellow'
        }
    }
];

var logLoc = function(idx) {
    if (idx >= fakeLocs.length) return;
    var loc = fakeLocs[idx];
    processEvent(loc);
    setTimeout(function() {
        logLoc(idx + 1);
    }, 500);
};

logLoc(0);
