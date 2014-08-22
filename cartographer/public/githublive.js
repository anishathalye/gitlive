var map = new Datamap({
    element: document.getElementById('map'),
    geographyConfig: {
        highlightOnHover: false,
        popupOnHover: false
    }
});

var geocoder = new google.maps.Geocoder();

var geocode = function(location, success, retries) {
    if (typeof(retries) === 'undefined') retries = 3;
    geocoder.geocode({'address': location}, function(results, status) {
        if (status == google.maps.GeocoderStatus.OK) {
            success(results);
        } else if (status == google.maps.GeocoderStatus.OVER_QUERY_LIMIT) {
            if (retries > 0) {
                setTimeout(function() {
                    geocode(location, success, retries - 1);
                }, 1000);
            }
        } else {
            // ignore
        }
    });
}

var locs = [];

var source = new EventSource('/events');
source.onmessage = function(e) {
    var event = JSON.parse(JSON.parse(e.data));
    geocode(event.fromLocation, function(from) {
        geocode(event.toLocation, function(to) {
            var fromLoc = {
                latitude: from[0].geometry.location.lat(),
                longitude: from[0].geometry.location.lng()
            };
            var toLoc = {
                latitude: to[0].geometry.location.lat(),
                longitude: to[0].geometry.location.lng()
            }
            locs.push({origin: fromLoc, destination: toLoc});
            map.arc(locs, {strokeWidth: 2});
        });
    });
};
