# for desktop only

_phaseAContext = (e) ->

  ###
  # Commonality; check for valid target
  ###
  element = e.target or e.currentTarget or e.srcElement
  if !element
    return
  console.log "Clicked element: #{element.tagName} #{element.className}"
  return

document.addEventListener 'contextmenu', _phaseAContext, true
