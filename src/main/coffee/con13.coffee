window.con13 = {
  channelDragStart: (ev, id) ->
    ev.dataTransfer.setData('channel', id)

  bridgeDragOver: (ev, id) ->
    ev.preventDefault()
    $('.drop-target').removeClass('drop-target')
    $("#bridge-#{id}").addClass('drop-target')

  bridgeDrop: (ev, bridgeId) ->
    channelId = ev.dataTransfer.getData('channel')
    console.log("Dropping #{channelId} on #{bridgeId}")
    ev.preventDefault()
    $("#bridge-#{bridgeId}").removeClass('drop-target')
    form = $("#bridgebuilder")
    form.find(".channel").val(channelId)
    form.find(".bridge").val(bridgeId)
    form.find(".build").submit()
}

