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
            strokeColor: '#95932e',
            fillColor: '#555400',
            borderColor: '#d2d199'
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
            strokeColor: '#95672e',
            fillColor: '#552f00',
            borderColor: '#d2b999'
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
            strokeColor: '#3f2565',
            fillColor: '#19033a',
            borderColor: '#7a6c8f'
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
