/* global agent */

let initializeAgent = function (tracer) {
    var counter = 0;

    agent.on('enter', function(ctx, frame) {
        const args = frame.args;
        if ('request' !== frame.type || args.length !== 2 || typeof args[0] !== 'object' || typeof args[1] !== 'object') {
            return;
        }
        const req = args[0];
        const res = args[1];
        const span = tracer.startSpan("request");
        span.setTag("span.kind", "server");
        span.setTag("http.url", req.url);
        span.setTag("http.method", req.method);
        res.id = ++counter;
        res.span = span;
        console.log(`agent: handling #${res.id} request for ${req.url}`);
    }, {
        roots: true,
        rootNameFilter: name => name === 'emit',
        sourceFilter: src => src.name === 'events.js'
    });

    agent.on('return', function(ctx, frame) {
        var res = frame['this'];
        if (res.span) {
            res.span.finish();
            console.log(`agent: finished #${res.id} request`);
        } else {
            // OK, caused for example by Tracer itself connecting to Jaeger server
            // console.warn(new Error("end of request with no active span").stack);
        }
    }, {
        roots: true,
        rootNameFilter: name => name === 'end',
        sourceFilter: src => src.name === '_http_outgoing.js'
    });
    console.log('agent: ready');
};

// register on a call to function tracerIsReady(tracer)
// that has to be defined by the application and called
// to give us a tracer to use in the agent
agent.on('enter', (ctx, frame) => {
    initializeAgent(frame.tracer);
}, {
    roots: true,
    rootNameFilter: (name) => name === 'tracerIsReady'
});

