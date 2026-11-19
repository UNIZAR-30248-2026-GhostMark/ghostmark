import { useEffect, useMemo, useRef } from "react";
import { ActivityIndicator, StyleSheet, View } from "react-native";
import { WebView } from "react-native-webview";

import { getConfiguredApiUrl } from "../api/client";
import { colors } from "../theme/colors";
import type { SafeZone, ZonePoint } from "../types/game";

type Props = {
  zones: SafeZone[];
  draftPoints?: ZonePoint[];
  editable?: boolean;
  height?: number;
  interactive?: boolean;
  onAddPoint?: (point: ZonePoint) => void;
};

/**
 * El WebView solo dibuja. Las zonas entran y salen por postMessage: nunca conoce la API
 * ni obtiene la ubicación del dispositivo.
 */
export function MapZones({ zones, draftPoints = [], editable = false, height = 300, interactive = true, onAddPoint }: Props) {
  const webview = useRef<WebView>(null);
  // El HTML se mantiene: al tocar un punto solo se actualizan las capas por postMessage.
  const html = useMemo(() => mapHtml(JSON.stringify({ draftPoints: [], editable, zones: [] })), [editable]);

  useEffect(() => {
    webview.current?.postMessage(JSON.stringify({ type: "render", value: { draftPoints, editable, zones } }));
  }, [draftPoints, editable, zones]);

  // Android no mide el WebView como contenido del padre: la altura ha de vivir en ambos.
  return <View style={[styles.frame, { height }]}>
    <WebView
      allowFileAccess={false}
      javaScriptEnabled
      onLoadEnd={() => webview.current?.postMessage(JSON.stringify({ type: "render", value: { draftPoints, editable, zones } }))}
      onMessage={(event) => {
        try {
          const message = JSON.parse(event.nativeEvent.data) as { type?: string; point?: ZonePoint };
          if (message.type === "point" && message.point && Number.isFinite(message.point.lat) && Number.isFinite(message.point.lon)) onAddPoint?.(message.point);
        } catch { /* Ignora mensajes que no proceden del mapa. */ }
      }}
      originWhitelist={["*"]}
      pointerEvents={interactive ? "auto" : "none"}
      ref={webview}
      renderLoading={() => <View style={styles.loading}><ActivityIndicator color={colors.lime} /></View>}
      // Las teselas de OpenStreetMap exigen Referer: el del servidor de GhostMark (diseño 10a.1).
      source={{ baseUrl: getConfiguredApiUrl(), html }}
      startInLoadingState
      style={styles.map}
    />
  </View>;
}

function mapHtml(initial: string) {
  return `<!doctype html><html lang="es"><head>
  <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" />
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" crossorigin="" />
  <style>html,body,#map{height:100%;margin:0;background:#e9dfc8}.leaflet-control-attribution{font:8px sans-serif}.leaflet-container{font-family:Arial,sans-serif}.zone-label{background:#fff8ec;border:2px solid #12061f;border-radius:8px;color:#12061f;font-weight:bold;padding:4px 9px;white-space:nowrap}.draft-preview{animation:draftPulse 1.05s ease-in-out infinite}@keyframes draftPulse{0%,100%{stroke-opacity:.28;fill-opacity:.04}50%{stroke-opacity:.85;fill-opacity:.12}}</style>
  </head><body><div id="map"></div><script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>
  <script>
    var data=${initial};
    var map=L.map('map',{zoomControl:true,attributionControl:true}).setView([41.6836,-0.8889],16);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap'}).addTo(map);
    var layer=L.layerGroup().addTo(map);
    var hasFitted=false;
    function points(xs){return xs.map(function(p){return [p.lat,p.lon]})}
    function draw(){
      layer.clearLayers(); var all=[];
      data.zones.forEach(function(zone,index){var latlngs=points(zone.puntos); all=all.concat(latlngs); var color=index%2?'#9B65BC':'#C3ADF3'; var polygon=L.polygon(latlngs,{color:'#12061f',weight:3,fillColor:color,fillOpacity:.53}).addTo(layer); zone.puntos.forEach(function(p){L.circleMarker([p.lat,p.lon],{radius:8,color:'#12061f',weight:3,fillColor:'#B8F24A',fillOpacity:1}).addTo(layer)}); var label=document.createElement('span');label.textContent=zone.nombre;polygon.bindTooltip(label,{permanent:true,direction:'center',className:'zone-label'});});
      if(data.editable && data.draftPoints.length){var draft=points(data.draftPoints); all=all.concat(draft); if(draft.length>=3){L.polygon(draft,{className:'draft-preview',color:'#12061F',dashArray:'8 7',fillColor:'#12061F',fillOpacity:.09,weight:3}).addTo(layer)}else{L.polyline(draft,{color:'#12061F',weight:3,dashArray:'5 5'}).addTo(layer)} draft.forEach(function(p){L.circleMarker(p,{radius:8,color:'#12061f',weight:3,fillColor:'#B8F24A',fillOpacity:1}).addTo(layer)});}
      if(!data.editable&&!hasFitted&&all.length){map.fitBounds(L.latLngBounds(all),{padding:[25,25],maxZoom:17});hasFitted=true;}
    }
    map.on('click',function(e){if(!data.editable)return; window.ReactNativeWebView.postMessage(JSON.stringify({type:'point',point:{lat:e.latlng.lat,lon:e.latlng.lng}}));});
    function receive(raw){try{var message=JSON.parse(raw); if(message.type==='render'){data=message.value;draw();}}catch(_){}}
    document.addEventListener('message',function(e){receive(e.data)});window.addEventListener('message',function(e){receive(e.data)});draw();
  </script></body></html>`;
}

const styles = StyleSheet.create({
  frame: { backgroundColor: "#E9DFC8", borderRadius: 19, overflow: "hidden" },
  map: { backgroundColor: "#E9DFC8", flex: 1, width: "100%" },
  loading: { alignItems: "center", backgroundColor: "#E9DFC8", flex: 1, justifyContent: "center" },
});
