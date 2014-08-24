var map = new Datamap({
    element: document.getElementById('map'),
    geographyConfig: {
        highlightOnHover: false,
        popupOnHover: false
    }
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

var processEvent = function(from, to) {
    locs.push({origin: from, destination: to});
    map.arcc(locs, {strokeWidth: 2});
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
            processEvent(fromLoc, toLoc);
        });
    });
};

var source = new EventSource('/events');
source.onmessage = function(e) {
    var event = JSON.parse(JSON.parse(e.data));
    processRawEvent(event);
};

var arccConfig = {
  strokeColor: '#DD1C77',
  strokeWidth: 1,
  arcSharpness: 1,
  animationSpeed: 600
}
          var bubbleOpts =  {
        borderWidth: 2,
        borderColor: '#FFFFFF',
        popupOnHover: true,
        popupTemplate: function(geography, data) {
          return '<div class="hoverinfo"><strong>' + data.name + '</strong></div>';
        },
        fillOpacity: 0.75,
        animate: true,
        highlightOnHover: true,
        highlightFillColor: '#0000FF',
        highlightBorderColor: 'rgba(0, 0, 255, 0.2)',
        highlightBorderWidth: 2,
        highlightFillOpacity: 0.85,
        exitDelay: 100
    };

  function handleArcc (layer, data, options) {
    var self = this,
        svg = this.svg;

    if ( !data || (data && !data.slice) ) {
      throw "Datamaps Error - arcs must be an array";
    }

    if ( typeof options === "undefined" || true ) {
      options = arccConfig;
    }

    var arcs = layer.selectAll('path.datamaps-arc').data( data, JSON.stringify );

    arcs
      .enter()
        .append('svg:path')
        .attr('class', 'datamaps-arc')
        .style('stroke-linecap', 'round')
        .style('stroke', function(datum) {
          if ( datum.options && datum.options.strokeColor) {
            return datum.options.strokeColor;
          }
          return  options.strokeColor
        })
        .style('fill', 'none')
        .style('stroke-width', function(datum) {
          if ( datum.options && datum.options.strokeWidth) {
            return datum.options.strokeWidth;
          }
          return options.strokeWidth;
        })
        .attr('d', function(datum) {
            var originXY = self.latLngToXY(datum.origin.latitude, datum.origin.longitude);
            var destXY = self.latLngToXY(datum.destination.latitude, datum.destination.longitude);
            var midXY = [ (originXY[0] + destXY[0]) / 2, (originXY[1] + destXY[1]) / 2];
            return "M" + originXY[0] + ',' + originXY[1] + "S" + (midXY[0] + (50 * options.arcSharpness)) + "," + (midXY[1] - (75 * options.arcSharpness)) + "," + destXY[0] + "," + destXY[1];
        })
        .attr('stroke-dasharray', function() {
            var length = this.getTotalLength();
            return length + ' ' + length;
        })
        .attr('stroke-dashoffset', function() {
            var length = this.getTotalLength();
            return length;
        })
        .transition().delay(400)
            .duration(600).ease('linear').attr('stroke-dashoffset', 0)

    arcs
      .enter()
        .append('svg:circle')
        .attr('class', 'datamaps-bubble')
        .attr('cx', function ( datum ) {
          var latLng;
          if ( datumHasCoords(datum.destination) ) {
            latLng = self.latLngToXY(datum.destination.latitude, datum.destination.longitude);
          }
          else if ( datum.centered ) {
            latLng = self.path.centroid(svg.select('path.' + datum.centered).data()[0]);
          }
          if ( latLng ) return latLng[0];
        })
        .attr('cy', function ( datum ) {
          var latLng;
          if ( datumHasCoords(datum.destination) ) {
            latLng = self.latLngToXY(datum.destination.latitude, datum.destination.longitude);
          }
          else if ( datum.centered ) {
            latLng = self.path.centroid(svg.select('path.' + datum.centered).data()[0]);
          }
          if ( latLng ) return latLng[1];;
        })
        .attr('r', 0) //for animation purposes
        .attr('data-info', function(d) {
          return JSON.stringify(d);
        })
        .style('stroke', function ( datum ) {
          return typeof datum.borderColor !== 'undefined' ? datum.borderColor : bubbleOpts.borderColor;
        })
        .style('stroke-width', function ( datum ) {
          return typeof datum.borderWidth !== 'undefined' ? datum.borderWidth : bubbleOpts.borderWidth;
        })
        .style('fill-opacity', function ( datum ) {
          return typeof datum.fillOpacity !== 'undefined' ? datum.fillOpacity : bubbleOpts.fillOpacity;
        })
        .style('fill', function ( datum ) {
          return '#0000FF';
        })
        .on('mouseover', function ( datum ) {
          var $this = d3.select(this);

          if (bubbleOpts.highlightOnHover) {
            //save all previous attributes for mouseout
            var previousAttributes = {
              'fill':  $this.style('fill'),
              'stroke': $this.style('stroke'),
              'stroke-width': $this.style('stroke-width'),
              'fill-opacity': $this.style('fill-opacity')
            };

            $this
              .style('fill', bubbleOpts.highlightFillColor)
              .style('stroke', bubbleOpts.highlightBorderColor)
              .style('stroke-width', bubbleOpts.highlightBorderWidth)
              .style('fill-opacity', bubbleOpts.highlightFillOpacity)
              .attr('data-previousAttributes', JSON.stringify(previousAttributes));
          }

          if (bubbleOpts.popupOnHover) {
            self.updatePopup($this, datum, bubbleOpts, svg);
          }
        })
        .on('mouseout', function ( datum ) {
          var $this = d3.select(this);

          if (bubbleOpts.highlightOnHover) {
            //reapply previous attributes
            var previousAttributes = JSON.parse( $this.attr('data-previousAttributes') );
            for ( var attr in previousAttributes ) {
              $this.style(attr, previousAttributes[attr]);
            }
          }

          d3.selectAll('.datamaps-hoverover').style('display', 'none');
        })
        .transition().delay(1000).duration(400)
          .attr('r', function ( datum ) {
              console.log(arguments);
            // return datum.radius;
            return 10;
          })
        .transition().delay(1400).duration(200)
          .attr('r', 5)

    arcs
      .enter()
        .append('svg:circle')
        .attr('class', 'datamaps-bubble')
        .attr('cx', function ( datum ) {
          var latLng;
          if ( datumHasCoords(datum.origin) ) {
            latLng = self.latLngToXY(datum.origin.latitude, datum.origin.longitude);
          }
          else if ( datum.centered ) {
            latLng = self.path.centroid(svg.select('path.' + datum.centered).data()[0]);
          }
          if ( latLng ) return latLng[0];
        })
        .attr('cy', function ( datum ) {
          var latLng;
          if ( datumHasCoords(datum.origin) ) {
            latLng = self.latLngToXY(datum.origin.latitude, datum.origin.longitude);
          }
          else if ( datum.centered ) {
            latLng = self.path.centroid(svg.select('path.' + datum.centered).data()[0]);
          }
          if ( latLng ) return latLng[1];;
        })
        .attr('r', 0) //for animation purposes
        .attr('data-info', function(d) {
          return JSON.stringify(d);
        })
        .style('stroke', function ( datum ) {
          return typeof datum.borderColor !== 'undefined' ? datum.borderColor : bubbleOpts.borderColor;
        })
        .style('stroke-width', function ( datum ) {
          return typeof datum.borderWidth !== 'undefined' ? datum.borderWidth : bubbleOpts.borderWidth;
        })
        .style('fill-opacity', function ( datum ) {
          return typeof datum.fillOpacity !== 'undefined' ? datum.fillOpacity : bubbleOpts.fillOpacity;
        })
        .style('fill', function ( datum ) {
          return '#0000FF';
        })
        .on('mouseover', function ( datum ) {
          var $this = d3.select(this);

          if (bubbleOpts.highlightOnHover) {
            //save all previous attributes for mouseout
            var previousAttributes = {
              'fill':  $this.style('fill'),
              'stroke': $this.style('stroke'),
              'stroke-width': $this.style('stroke-width'),
              'fill-opacity': $this.style('fill-opacity')
            };

            $this
              .style('fill', bubbleOpts.highlightFillColor)
              .style('stroke', bubbleOpts.highlightBorderColor)
              .style('stroke-width', bubbleOpts.highlightBorderWidth)
              .style('fill-opacity', bubbleOpts.highlightFillOpacity)
              .attr('data-previousAttributes', JSON.stringify(previousAttributes));
          }

          if (bubbleOpts.popupOnHover) {
            self.updatePopup($this, datum, bubbleOpts, svg);
          }
        })
        .on('mouseout', function ( datum ) {
          var $this = d3.select(this);

          if (bubbleOpts.highlightOnHover) {
            //reapply previous attributes
            var previousAttributes = JSON.parse( $this.attr('data-previousAttributes') );
            for ( var attr in previousAttributes ) {
              $this.style(attr, previousAttributes[attr]);
            }
          }

          d3.selectAll('.datamaps-hoverover').style('display', 'none');
        })
        .transition().duration(400)
          .attr('r', function ( datum ) {
              console.log(arguments);
            // return datum.radius;
            return 10;
          })
        .transition().delay(400).duration(200)
          .attr('r', 5)

    arcs.exit()
      .transition()
      .style('opacity', 0)
      .remove();
  }
    function datumHasCoords (datum) {
      return typeof datum !== 'undefined' && typeof datum.latitude !== 'undefined' && typeof datum.longitude !== 'undefined';
    }

  map.addPlugin('arcc', handleArcc);
