window.con13 = {
  channelDragStart: (ev, id) ->
    ev.dataTransfer.setData('channel', id)

  bridgeDragOver: (ev, id) ->
    ev.preventDefault()
    $('.drop-target').removeClass('drop-target')
    $("#bridge-#{id}").addClass('drop-target')

  bridgeDrop: (ev, id) ->
    console.log("Dropping #{ev.dataTransfer.getData('channel')} on #{id}")
    ev.preventDefault()
    $("#bridge-#{id}").removeClass('drop-target')
}

