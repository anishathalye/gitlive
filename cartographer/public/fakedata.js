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
            strokeColor: '#009999',
            fillColor: '#007777',
            borderColor: '#46b3b3'
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
            strokeColor: '#e20048',
            fillColor: '#b00038',
            borderColor: '#ea5b89'
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
            strokeColor: '#00b945',
            fillColor: '#008f35',
            borderColor: '#4fcb7d'
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
            strokeColor: '#ffba00',
            fillColor: '#c69000',
            borderColor: '#ffd564'
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
