var fakeLocs = [
    [
        {
            latitude: 50,
            longitude: 30
        },
        {
            latitude: 60,
            longitude: 40
        }
    ],
    [
        {
            latitude: -30,
            longitude: 30
        },
        {
            latitude: 0,
            longitude: -5
        }
    ],
    [
        {
            latitude: -15,
            longitude: -20
        },
        {
            latitude: 20,
            longitude: -5
        }
    ],
    [
        {
            latitude: 60,
            longitude: -120
        },
        {
            latitude: 20,
            longitude: -80
        }
    ]
];

var logLoc = function(idx) {
    if (idx >= fakeLocs.length) return;
    var loc = fakeLocs[idx];
    processEvent(loc[0], loc[1]);
    setTimeout(function() {
        logLoc(idx + 1);
    }, 500);
};

logLoc(0);
