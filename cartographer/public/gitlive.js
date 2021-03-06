var map = new Datamap({
    element: document.getElementById('map'),
    geographyConfig: {
        highlightOnHover: false,
        popupOnHover: false,
        borderColor: '#555555',
        borderWidth: 1
    },
    fills: {
        defaultFill: '#2e2e2e'
    },
});

// have map intercept clicks / taps
document.getElementById('map').addEventListener('click', function() {});

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
                }, 2000);
            } else {
                window.location.reload();
            }
        } else {
            // ignore
        }
    });
}

var id = 0;

var locs = [];

var removeLoc = function(data) {
    var index;
    for (var i = 0, len = locs.length; i < len; i++) {
        if (locs[i].id === data.id) {
            index = i;
            break;
        }
    }
    locs.splice(index, 1);
}

var TIMEOUT = 60000;

var processEvent = function(data) {
    var flip = function(data) {
        var tmp = data.origin;
        data.origin = data.destination;
        data.destination = tmp;
    }
    switch(data.type) {
        case 'WatchEvent':
            data.options = { color: 'yellow' };
            break;
        case 'ForkEvent':
            data.options = { color: 'blue' };
            break;
        case 'PullRequestEvent':
            data.options = { color: 'green' };
            break;
        case 'IssuesEvent':
            data.options = { color: 'red' };
            break;
    }
    data.id = id++;
    data.timeoutTime = TIMEOUT;
    data.timeoutFunc = function() {
        removeLoc(data);
        map.linez(locs);
    }
    data.timeout = setTimeout(data.timeoutFunc, data.timeoutTime);
    locs.push(data);
    map.linez(locs);
};

var processRawEvent = function(event) {
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
            event.origin = fromLoc;
            event.destination = toLoc;
            processEvent(event);
        });
    });
};

if (typeof(EventSource) !== 'undefined') {
    var source = new EventSource('/events');
    source.onmessage = function(e) {
        var event = JSON.parse(JSON.parse(e.data));
        processRawEvent(event);
    };
} else {
    alert('Your browser is not supported. You must use a browser that supports the EventSource API.');
}

var escapeHtml = function(str) {
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
}

var linezOptions = {
    arcSharpness: 1,
    highlightBorderWidth: 3,
    popupTemplate: function(data) {
        // remember to escape data
        var user = escapeHtml(data.fromLogin);
        var from = escapeHtml(data.fromLocation);
        var repo = escapeHtml(data.toLogin) + '/' + escapeHtml(data.toRepo);
        var to = escapeHtml(data.toLocation);
        var message;
        switch (data.type) {
            case 'WatchEvent':
                message = '<span class="yellow">starred</span>';
                break;
            case 'ForkEvent':
                message = '<span class="blue">forked</span>';
                break;
            case 'PullRequestEvent':
                message = 'sent a <span class="green">pull request</span> to';
                break;
            case 'IssuesEvent':
                message = 'opened an <span class="red">issue</span> at';
                break;
        }
        return user + ' <em>(' + from + ')</em> ' + message + ' ' + repo + ' <em>(' + to + ')</em>';
    }
};

var mobileDevice = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);

var handleLinez = function(layer, data) {
    var self = this;
    var svg = this.svg;

    var tip = d3.tip().attr('class', 'd3-tip').offset([-10, 0]).html(linezOptions.popupTemplate);
    layer.call(tip);

    var linez = layer.selectAll('.githublive-linez').data(data, JSON.stringify);

    var container = linez.enter().append('g').attr('class', 'githublive-linez');

    var back = container.append('g');
    var front = container.append('g');

    front.append('svg:circle')
        .attr('class', 'githublive-linez-circle')
        .attr('cx', function(datum) {
            var latLng = self.latLngToXY(datum.origin.latitude, datum.origin.longitude);
            if (latLng) {
                return latLng[0];
            }
        })
        .attr('cy', function(datum) {
            var latLng = self.latLngToXY(datum.origin.latitude, datum.origin.longitude);
            if (latLng) {
                return latLng[1];
            }
        })
        .attr('r', 0)
        .attr('data-info', function(d) {
            return JSON.stringify(d);
        })
        .attr('class', function(datum) {
            var previous = d3.select(this).attr('class');
            if (datum.options && datum.options.color) {
                var ret = previous + ' ' + datum.options.color;
                return ret;
            }
            return previous + ' default';
        })
        .on('mouseover', function(datum) {
            clearTimeout(datum.timeout);

            tip.show(datum);
        })
        .on('mouseout', function(datum) {
            datum.timeout = setTimeout(datum.timeoutFunc, datum.timeoutTime);

            tip.hide(datum);
        })
        .on('click', function(datum) {
            if (!mobileDevice) {
                window.open(datum.url, '_blank');
            }
        })
        .transition().delay(0).duration(400)
            .attr('r', 10)
        .transition().delay(400).duration(200)
            .attr('r', 5);

    back.append('svg:path')
        .attr('class', 'githublive-linez-path')
        .attr('class', function(datum) {
            var previous = d3.select(this).attr('class');
            if (datum.options && datum.options.color) {
                var ret = previous + ' ' + datum.options.color;
                return ret;
            }
            return previous + ' default';
        })
        .attr('d', function(datum) {
            var originXY = self.latLngToXY(datum.origin.latitude, datum.origin.longitude);
            var destXY = self.latLngToXY(datum.destination.latitude, datum.destination.longitude);
            var midXY = [ (originXY[0] + destXY[0]) / 2, (originXY[1] + destXY[1]) / 2];
            return "M" + originXY[0] + ',' + originXY[1] + "S" + (midXY[0] + (50 * linezOptions.arcSharpness)) + "," + (midXY[1] - (75 * linezOptions.arcSharpness)) + "," + destXY[0] + "," + destXY[1];
        })
        .attr('stroke-dasharray', function() {
            var length = this.getTotalLength();
            return length + ' ' + length;
        })
        .attr('stroke-dashoffset', function() {
            var length = this.getTotalLength();
            return length;
        })
        .transition()
            .delay(400)
            .duration(600)
            .ease('linear')
            .attr('stroke-dashoffset', 0);

    front.append('svg:circle')
        .attr('class', 'githublive-linez-circle')
        .attr('cx', function(datum) {
            var latLng = self.latLngToXY(datum.destination.latitude, datum.destination.longitude);
            if (latLng) {
                return latLng[0];
            }
        })
        .attr('cy', function(datum) {
            var latLng = self.latLngToXY(datum.destination.latitude, datum.destination.longitude);
            if (latLng) {
                return latLng[1];
            }
        })
        .attr('r', 0)
        .attr('data-info', function(d) {
            return JSON.stringify(d);
        })
        .attr('class', function(datum) {
            var previous = d3.select(this).attr('class');
            if (datum.options && datum.options.color) {
                var ret = previous + ' ' + datum.options.color;
                return ret;
            }
            return previous + ' default';
        })
        .on('mouseover', function(datum) {
            clearTimeout(datum.timeout);

            tip.show(datum);
        })
        .on('mouseout', function(datum) {
            datum.timeout = setTimeout(datum.timeoutFunc, datum.timeoutTime);

            tip.hide(datum);
        })
        .on('click', function(datum) {
            if (!mobileDevice) {
                window.open(datum.url, '_blank');
            }
        })
        .transition().delay(1000).duration(400)
            .attr('r', 10)
        .transition().delay(1400).duration(200)
            .attr('r', 5);

    linez.exit()
        .transition()
        .style('opacity', 0)
        .remove();
};

map.addPlugin('linez', handleLinez);
