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

// var geocoder = new google.maps.Geocoder();

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

var processEvent = function(data) {
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
            processEvent(data);
        });
    });
};

var source = new EventSource('/events');
source.onmessage = function(e) {
    var event = JSON.parse(JSON.parse(e.data));
    processRawEvent(event);
};

var linezOptions = {
    strokeColor: '#111111',
    fillColor: '#555555',
    strokeWidth: 2,
    arcSharpness: 1,
    borderWidth: 2,
    highlightBorderWidth: 3,
    borderColor: '#FFFFFF',
    popupTemplate: function(geography, data) {
        // remember to escape data
        return '<div class="hoverinfo">' + '<strong>this</strong> <em>is</em> a test' + '</div>';
    },
    fillOpacity: 1,
    highlightFillOpacity: 1
};

var handleLinez = function(layer, data) {
    var self = this;
    var svg = this.svg;

    var linez = layer.selectAll('path.githublive-linez').data(data, JSON.stringify);

    var back = linez.enter().append('g');
    var front = linez.enter().append('g');

    front.append('svg:circle')
        .attr('class', 'githublive-linez')
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
        .style('stroke', function(datum) {
            if (datum.options && datum.options.borderColor) {
                return datum.options.borderColor;
            }
            return linezOptions.borderColor;
        })
        .style('stroke-width', linezOptions.borderWidth)
        .style('fill-opacity', linezOptions.fillOpacity)
        .style('fill', function(datum) {
            if (datum.options && datum.options.fillColor) {
                return datum.options.fillColor;
            }
            return linezOptions.fillColor;
        })
        .on('mouseover', function(datum) {
            var $this = d3.select(this);

            var previousAttributes = {
                'fill': $this.style('fill'),
                'stroke': $this.style('stroke'),
                'stroke-width': $this.style('stroke-width'),
                'fill-opacity': $this.style('fill-opacity')
            };

            $this
                .style('fill-opacity', linezOptions.highlightFillOpacity)
                .style('stroke-width', linezOptions.highlightBorderWidth)
                .attr('data-previousAttributes', JSON.stringify(previousAttributes));

            self.updatePopup($this, datum, linezOptions, svg);
        })
        .on('mouseout', function(datum) {
            var $this = d3.select(this);
            
            var previousAttributes = JSON.parse($this.attr('data-previousAttributes'));
            for (var attr in previousAttributes) {
                $this.style(attr, previousAttributes[attr]);
            }

            d3.selectAll('.datamaps-hoverover').style('display', 'none');
        })
        .transition().delay(0).duration(400)
            .attr('r', 10)
        .transition().delay(400).duration(200)
            .attr('r', 5);

    back.append('svg:path')
        .attr('class', 'githublive-linez')
        .style('stroke-linecap', 'round')
        .style('stroke', function(datum) {
            if (datum.options && datum.options.strokeColor) {
                return datum.options.strokeColor;
            }
            return linezOptions.strokeColor;
        })
        .style('fill', 'none')
        .style('stroke-width', linezOptions.strokeWidth)
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
        .attr('class', 'githublive-linez')
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
        .style('stroke', function(datum) {
            if (datum.options && datum.options.borderColor) {
                return datum.options.borderColor;
            }
            return linezOptions.borderColor;
        })
        .style('stroke-width', linezOptions.borderWidth)
        .style('fill-opacity', linezOptions.fillOpacity)
        .style('fill', function(datum) {
            if (datum.options && datum.options.fillColor) {
                return datum.options.fillColor;
            }
            return linezOptions.fillColor;
        })
        .on('mouseover', function(datum) {
            var $this = d3.select(this);

            var previousAttributes = {
                'fill': $this.style('fill'),
                'stroke': $this.style('stroke'),
                'stroke-width': $this.style('stroke-width'),
                'fill-opacity': $this.style('fill-opacity')
            };

            $this
                .style('fill-opacity', linezOptions.highlightFillOpacity)
                .style('stroke-width', linezOptions.highlightBorderWidth)
                .attr('data-previousAttributes', JSON.stringify(previousAttributes));

            self.updatePopup($this, datum, linezOptions, svg);
        })
        .on('mouseout', function(datum) {
            var $this = d3.select(this);
            
            var previousAttributes = JSON.parse($this.attr('data-previousAttributes'));
            for (var attr in previousAttributes) {
                $this.style(attr, previousAttributes[attr]);
            }

            d3.selectAll('.datamaps-hoverover').style('display', 'none');
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