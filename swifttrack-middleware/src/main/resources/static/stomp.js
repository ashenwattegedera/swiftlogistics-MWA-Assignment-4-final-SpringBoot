// Minimal STOMP 1.2 client over native WebSocket (no external dependencies).
// Sufficient for the SwiftTrack demo portal/driver UI.
function StompClient(url) {
    this.url = url;
    this.ws = null;
    this.connected = false;
    this.subscriptions = {};
    this.subCounter = 0;
    this.onConnect = null;
}

StompClient.prototype.connect = function (onConnect) {
    var self = this;
    this.onConnect = onConnect;
    this.ws = new WebSocket(this.url);

    this.ws.onopen = function () {
        self.ws.send('CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\0');
    };

    this.ws.onmessage = function (evt) {
        self.handle(evt.data);
    };

    this.ws.onclose = function () {
        self.connected = false;
    };
};

StompClient.prototype.handle = function (data) {
    var idx = data.indexOf('\n\n');
    if (idx < 0) { return; }
    var head = data.substring(0, idx);
    var rest = data.substring(idx + 2);
    var body = rest.replace(/\0+$/, '');
    var lines = head.split('\n');
    var command = lines[0];
    var headers = {};
    for (var i = 1; i < lines.length; i++) {
        var p = lines[i].indexOf(':');
        if (p > 0) { headers[lines[i].substring(0, p)] = lines[i].substring(p + 1); }
    }

    if (command === 'CONNECTED') {
        this.connected = true;
        if (this.onConnect) { this.onConnect(); }
    } else if (command === 'MESSAGE') {
        var handler = this.subscriptions[headers['destination']];
        if (handler) {
            var payload = body;
            try { payload = JSON.parse(body); } catch (e) { /* keep raw string */ }
            handler(payload, headers);
        }
    } else if (command === 'ERROR') {
        console.error('STOMP ERROR', body);
    }
};

StompClient.prototype.subscribe = function (destination, handler) {
    this.subscriptions[destination] = handler;
    var id = 'sub-' + (++this.subCounter);
    this.ws.send('SUBSCRIBE\nid:' + id + '\ndestination:' + destination + '\nack:auto\n\n\0');
};

StompClient.prototype.disconnect = function () {
    if (this.ws) { this.ws.close(); }
};
