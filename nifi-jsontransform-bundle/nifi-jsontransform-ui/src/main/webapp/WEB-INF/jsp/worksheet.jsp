<%@ page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<!DOCTYPE html>
<!DOCTYPE html>
<html>
<head>
    <title>Jolt Transform Demo</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <script type="text/javascript" src="../nifi/js/jquery/jquery-2.1.1.min.js"></script>
    <script type="text/javascript" src="../nifi/js/jquery/jquery.center.js"></script>
    <script type="text/javascript" src="../nifi/js/jquery/jquery.each.js"></script>
    <script type="text/javascript" src="../nifi/js/jquery/jquery.tab.js"></script>
    <script type="text/javascript" src="../nifi/js/jquery/modal/jquery.modal.js"></script>
    <script type="text/javascript" src="../nifi/js/jquery/combo/jquery.combo.js"></script>
    <script type="text/javascript" src="../nifi/js/jquery/jquery.ellipsis.js"></script>
    <script type="text/javascript" src="../nifi/js/jquery/ui-smoothness/jquery-ui-1.10.4.min.js"></script>


    <!-- Use the cdn hosted bootstrap -->

    <!-- Latest compiled and minified CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css">

    <!-- Optional theme -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap-theme.min.css">

    <!-- Latest compiled and minified JavaScript -->
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/js/bootstrap.min.js"></script>

    <style>
        /* This makes the panels have some padding from the edge of the screen. */
        @media (min-width: 600px) {
                body {
                    padding-right:25px;
                    padding-left:25px;
                }
            }
        @media (max-width: 599px) {
                body {
                    padding-right:2px;
                    padding-left:2px;
                }
            }

        /* This makes the textAreas flush with the containing panels. */
        .panel-body {
            padding: 0px;
        }
    </style>

</head>
<body>
<div id="transformjson-processor-id" class="hidden"><%= request.getParameter("id") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("id")) %></div>
<div id="transformjson-client-id" class="hidden"><%= request.getParameter("clientId") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("clientId")) %></div>
<div id="transformjson-revision" class="hidden"><%= request.getParameter("revision") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("revision")) %></div>
<div id="transformjson-editable" class="hidden"><%= request.getParameter("editable") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("editable")) %></div>

<div class="row"> <!-- row 1 -->
<div class="col-lg-12">
<h1>TransformJson Sandbox</h1>
Here you can experiment with the Jolt Transforms before saving them into the transformation processor config.
</div>
</div> <!-- row 1 -->

<form id="transformForm" role="form" action="api/transformations/transform" method="post">

<div class="row"> <!-- row 2 -->
<div class="col-lg-4 col-md-6 col-sm-12">

        <div id="inputPanel" class="panel panel-default">
        <div class="panel-heading">
            <label for="input">Json Input</label>
            <button type="button" id="inputValidateButton" class="btn btn-default">JSON Validate</button>
        </div>
        <div class="panel-body">
            <textarea form="transformForm" class="form-control" name="input" id="input" rows="20">
            </textarea>
        </div>
    </div>
</div> <!-- column 1 -->

<div class="col-lg-4 col-md-6 col-sm-12">

    <div id="specPanel" class="panel panel-default">
        <div class="panel-heading">
            <label for="spec">Jolt Spec</label>
            <button type="button" id="specValidateButton" class="btn btn-default">JSON Validate</button>
            <!-- <button type="button" id="specSaveButton" class="btn btn-default">Save</button>
            <span id="message"></span>-->
        </div>
        <div class="panel-body">
            <textarea form="transformForm" class="form-control" id="spec" rows="20">
            </textarea>
        </div>
    </div>
</div> <!-- column 2 -->

<div class="col-lg-4 col-md-6 col-sm-12">

    <div id="outputPanel" class="panel panel-default">

        <div class="panel-heading">
            <label for="outputJson">Output / Errors</label>
            <button id="processButton" name="processButton" type="submit" class="btn btn-default">Test Transform</button>
        </div>
        <div class="panel-body">
            <textarea form="transformForm" class="form-control" id="outputJson" rows="20">
            </textarea>
        </div>
    </div>
</div> <!-- column 3 -->
</div> <!-- row 2 -->
<div class="row">
    <div>
        <div class="col-lg-11 text-right" id="message"></div>
        <div class="col-lg-1">
            <button type="button" id="specSaveButton" class="btn btn-default">Save</button></div>
        <div class="clear"></div>
    </div>
</div>
</form>
<div id="ok-dialog">
    <div id="ok-dialog-content" class="dialog-content"></div>
</div>
<div id="yes-no-dialog">
    <div id="yes-no-dialog-content" class="dialog-content"></div>
</div>
<!-- Load up the Core CodeMirror JavaScript and CSSs. -->
<script src="js/codemirror.js"></script>
<script src="js/javascript.js"></script>
<link rel="stylesheet" href="css/codemirror.css">
<link rel="stylesheet" href="css/eclipse.css">

<!-- Load up the Lint stuff : This will show nice red errors on JSON mis-format. -->
<script src="js/lint.js"></script>  <!-- core lib -->
<script src="js/jsonlint.js"></script>  <!-- zaach/jsonlint lib-->
<script src="js/json-lint.js"></script>  <!-- Wrapper for the zaach/jsonlint lib-->
<link rel="stylesheet" href="css/lint.css">

<!-- Load up the extra goodies : MatchBrackets highlights opening and closing { } -->
<script src="js/matchbrackets.js"></script>

<!-- Script at the bottom so that we know that html elements have been loaded. -->
<script type="text/javascript">

var codeMirrorOptions =
{
    "mode": {
        name: "javascript",
        json: true
    },
    "theme" : "eclipse",
    "tabSize" : 2,
    "indentUnit" : 2,
    "smartIndent" : true,
    "lineNumbers" : true,
    "lint" : true,
    "matchBrackets": true
};

// #input finds an html element by Id, then [0] gets the first thing in the returned list
var inputCodeMirror   = CodeMirror.fromTextArea( $( "#input" )[0], codeMirrorOptions );
var specCodeMirror    = CodeMirror.fromTextArea( $( "#spec" )[0],  codeMirrorOptions );
var outputCodeMirror  = CodeMirror.fromTextArea( $( "#outputJson" )[0],  codeMirrorOptions );

// this is awesome, makes it so the textAreas auto size their height
// the width is 100% which allows it to be controlled by the BootStrap logic
inputCodeMirror.setSize( "100%", "auto");
specCodeMirror.setSize( "100%", "auto");
outputCodeMirror.setSize( "100%", "auto");

var evaluationContext = $.ajax({
    type: 'GET',
    url: 'api/transformations/evaluation-context?' + $.param({
        processorId: $('#transformjson-processor-id').text()
    })
}).done(function (evaluationContext) {
    // populate the controls
    inputCodeMirror.setValue(evaluationContext.sampleInput)
    specCodeMirror.setValue(evaluationContext.joltTransform)
});

// Poor man's Json Validation
function parseJson( json ) {
    try {
        var theJson = jQuery.parseJSON( json );
        return theJson;
    }
    catch (e) {
        return false;
    }
}

function validateInput() {

    var inputPanel = $("#inputPanel");
    var theJson = parseJson( inputCodeMirror.getValue() );
    if ( theJson ) {
        inputPanel.toggleClass("panel-danger", false);
        inputCodeMirror.setValue( JSON.stringify( theJson, null, '\t' ) );
        return true;
    }
    else {
        inputPanel.toggleClass("panel-danger", true);
        return false;
    }
}

$("#inputValidateButton").click( validateInput );

function validateSpec() {

    var specPanel = $("#specPanel");

    var theJson = parseJson( specCodeMirror.getValue() );
    if ( theJson ) {
        specPanel.toggleClass("panel-danger", false);
        specCodeMirror.setValue( JSON.stringify( theJson, null, '\t' ) );
        return true;
    }
    else {
        specPanel.toggleClass("panel-danger", true);
    }
    return false;
}

$("#specValidateButton").click( validateSpec );

/*
 * Attach a submit handler to the form.
 * The form should still theoretically work even w/out JavaScript.
 */
$("#transformForm").submit(function(event) {

    var inputValid = validateInput();
    var specValid = validateSpec();
    if ( ! inputValid || ! specValid ) {
        outputCodeMirror.setValue( "Problem with input or spec JSON." );

        // return false so that we prevent the standard form submit behavior from happening
        return false;
    }

    // get some values from elements on the page:
    var $form = $( this ),
        input = inputCodeMirror.getValue(),
        spec  = specCodeMirror.getValue(),
        url   = $form.attr( 'action' );

    outputCodeMirror.setValue( "Sending 'input' and 'spec' to the server." );

    // Send the data using post
    var posting = $.post( url, { input : input, spec : spec } );

    // Put the results in a div
    posting.done(function( data ) {
        var content = data ;
        outputCodeMirror.setValue( content );
    }).fail(function( data ) {
        var content = data.responseText ;
        outputCodeMirror.setValue( content );
    });

    // return false so that we prevent the standard form submit behavior from happening
    return false;
});

function showMessage (text) {
        $('#message').text(text);
        setTimeout(function () {
            $('#message').text('');
        }, 10000);
    }

function saveTransform() {


        var entity = {
            processorId: $('#transformjson-processor-id').text(),
            revision: $('#transformjson-revision').text(),
            clientId: $('#transformjson-client-id').text(),
            joltTransform: specCodeMirror.getValue(),
            sampleInput: inputCodeMirror.getValue()
        };

        return $.ajax({
            type: 'PUT',
            url: 'api/transformations/evaluation-context',
            data: JSON.stringify(entity),
            processData: false,
            contentType: 'application/json'
        }).then(function () {
            showMessage('Transform saved.');
        }, function (xhr, status, error) {
            // show an error message
            $('#ok-dialog-content').text('Unable to save transform due to:' + xhr.responseText);
            $('#ok-dialog').modal('setHeaderText', 'Error').modal('show');
        });
    }

    $("#specSaveButton").click( saveTransform );
</script>

</body>
</html>

