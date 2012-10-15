

var mbUrl = 'http://{s}.tiles.mapbox.com/v3/openplans.map-zugfpt95/{z}/{x}/{y}.png';

var overlayUrl = 'http://cebutraffic.org/tiles/{z}/{x}/{y}.png';


var mbAttrib = 'Traffic overlay powered by OpenPlans Vehicle Tracking Tools, Map tiles &copy; Mapbox (terms).';
var mbOptions = {
  maxZoom : 17,
  attribution : mbAttrib
};

// dynamic height management

$(document).ready(sizeContent);
$(window).resize(sizeContent);

function sizeContent() {
  var newHeight = $(window).height() - $("#header").height() + "px";
  $("#map").css("height", newHeight);
}
	
var traceLayer = new L.LayerGroup();
var traceData = new Array();

function loadTraces()
{
	$.get('/api/traces', function(data){
		traceData = data;
		
		updateTraces();
	});
}

function updateTraces()
{
	traceLayer.clearLayers();
	
	for(var trace in traceData)
	{
		var error  = 2;
		if(traceData[trace].gpsError > 0 )
			error = traceData[trace].gpsError;
		if (traceData[trace].gpsError < 50)
			L.circle([traceData[trace].lat, traceData[trace].lon],  error).addTo(traceLayer);
		
	}
}

function refreshTiles()
{
	map.invalidateSize();
}

// main 

$(document).ready(function() {
	
  map = new L.map('map').setView([38.90961,-77.01576], 13);

  L.tileLayer(mbUrl, mbOptions).addTo(map);
  
  //L.tileLayer(overlayUrl, mbOptions).addTo(map);
  
  //traceLayer.addTo(map);
  
  //loadTraces();
  
  //setInterval(loadTraces, 10000);
  //setInterval(refreshTiles, 30000);

});