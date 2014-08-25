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
            strokeColor: '#b98038',
            fillColor: '#794300',
            borderColor: '#f6dec0'
        }
    },
    {
        origin: {
            latitude: -30,
            longitude: 30
        },
        destination: {
            latitude: 0,
            longitude: -5
        },
        options: {
            strokeColor: '#38982e',
            fillColor: '#096300',
            borderColor: '#a2ca9d'
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
            strokeColor: '#b6373e',
            fillColor: '#760006',
            borderColor: '#f2bdc0'
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
            strokeColor: '#b9a238',
            fillColor: '#796300',
            borderColor: '#f6ecc0'
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
