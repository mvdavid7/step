/*******************************************************************************
 * Copyright (c) 2012, Directors of the Tyndale STEP Project All rights
 * reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. Neither the name of the Tyndale House, Cambridge
 * (www.TyndaleHouse.com) nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
step.navigation = {
	showBottomSection: function(menuItemOrPassageId) {
	    var passageId = menuItemOrPassageId;
	    if(isNaN(parseInt(menuItemOrPassageId))) {
	        passageId = step.passage.getPassageId(menuItemOrPassageId);
	    }
	    
	    if (passageId == 0) {
			var verse = $('#leftPassageReference').val();
			$('.timelineContext:first').html(verse);
		} else {
			var verse = $('#rightPassageReference').val();
			$('.timelineContext:first').html(verse);	
		}
	
		var bottomSection = $("#bottomSection");
		var bottomSectionContent = $("#bottomSectionContent");
		
		bottomSection.show();
		bottomSection.height(250);
		bottomSectionContent.height(225);
		
		refreshLayout();
	},
	
	hideBottomSection: function() {
		var bottomSection = $("#bottomSection");
		var bottomSectionContent = $("#bottomSectionContent");

		bottomSection.hide();
		bottomSection.height(0);
		bottomSectionContent.height(0);
		
		refreshLayout();
	}
};
