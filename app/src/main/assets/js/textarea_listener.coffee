# focus listener for textareas
# since swipe to refresh is quite sensitive, we will disable it
# when we detect a user typing
# note that this extends passed having a keyboard opened,
# as a user may still be reviewing his/her post
# swiping should automatically be reset on refresh

_phaseFocus = (e) ->
    element = e.target or e.srcElement
    console.log "Phase focus", element.tagName
    if element.tagName == "TEXTAREA"
        Phase?.disableSwipeRefresh true
    return

_phaseBlur = (e) ->
    element = e.target or e.srcElement
    console.log "Phase blur", element.tagName
    Phase?.disableSwipeRefresh false
    return

document.addEventListener "focus", _phaseFocus, true
document.addEventListener "blur", _phaseBlur, true
