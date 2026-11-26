"use strict";
// Copyright 2019 Google Inc. Use of this source code is governed by an
// MIT-style license that can be found in the LICENSE file or at
// https://opensource.org/licenses/MIT.
Object.defineProperty(exports, "__esModule", { value: true });
exports.ListSeparatorSchema = exports.ListSeparator = exports.ProtocolErrorTypeSchema = exports.ProtocolErrorType = exports.LogEventTypeSchema = exports.LogEventType = exports.SyntaxSchema = exports.Syntax = exports.OutputStyleSchema = exports.OutputStyle = exports.NodePackageImporterSchema = exports.Value_Calculation_CalculationOperationSchema = exports.Value_Calculation_CalculationValueSchema = exports.Value_CalculationSchema = exports.Value_ArgumentListSchema = exports.Value_CompilerModuleSchema = exports.Value_CompilerMixinSchema = exports.Value_HostFunctionSchema = exports.Value_CompilerFunctionSchema = exports.Value_Map_EntrySchema = exports.Value_MapSchema = exports.Value_ListSchema = exports.Value_ColorSchema = exports.Value_NumberSchema = exports.Value_StringSchema = exports.ValueSchema = exports.SourceSpan_SourceLocationSchema = exports.SourceSpanSchema = exports.ProtocolErrorSchema = exports.OutboundMessage_FunctionCallRequestSchema = exports.OutboundMessage_FileImportRequestSchema = exports.OutboundMessage_ImportRequestSchema = exports.OutboundMessage_CanonicalizeRequestSchema = exports.OutboundMessage_LogEventSchema = exports.OutboundMessage_CompileResponse_CompileFailureSchema = exports.OutboundMessage_CompileResponse_CompileSuccessSchema = exports.OutboundMessage_CompileResponseSchema = exports.OutboundMessage_VersionResponseSchema = exports.OutboundMessageSchema = exports.InboundMessage_FunctionCallResponseSchema = exports.InboundMessage_FileImportResponseSchema = exports.InboundMessage_ImportResponse_ImportSuccessSchema = exports.InboundMessage_ImportResponseSchema = exports.InboundMessage_CanonicalizeResponseSchema = exports.InboundMessage_CompileRequest_ImporterSchema = exports.InboundMessage_CompileRequest_StringInputSchema = exports.InboundMessage_CompileRequestSchema = exports.InboundMessage_VersionRequestSchema = exports.InboundMessageSchema = exports.file_embedded_sass = void 0;
exports.CalculationOperatorSchema = exports.CalculationOperator = exports.SingletonValueSchema = exports.SingletonValue = void 0;
const codegenv2_1 = require("@bufbuild/protobuf/codegenv2");
/**
 * Describes the file embedded_sass.proto.
 */
exports.file_embedded_sass = (0, codegenv2_1.fileDesc)("ChNlbWJlZGRlZF9zYXNzLnByb3RvEhZzYXNzLmVtYmVkZGVkX3Byb3RvY29sIooRCg5JbmJvdW5kTWVzc2FnZRJQCg9jb21waWxlX3JlcXVlc3QYAiABKAsyNS5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLkluYm91bmRNZXNzYWdlLkNvbXBpbGVSZXF1ZXN0SAASXAoVY2Fub25pY2FsaXplX3Jlc3BvbnNlGAMgASgLMjsuc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5JbmJvdW5kTWVzc2FnZS5DYW5vbmljYWxpemVSZXNwb25zZUgAElAKD2ltcG9ydF9yZXNwb25zZRgEIAEoCzI1LnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuSW5ib3VuZE1lc3NhZ2UuSW1wb3J0UmVzcG9uc2VIABJZChRmaWxlX2ltcG9ydF9yZXNwb25zZRgFIAEoCzI5LnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuSW5ib3VuZE1lc3NhZ2UuRmlsZUltcG9ydFJlc3BvbnNlSAASXQoWZnVuY3Rpb25fY2FsbF9yZXNwb25zZRgGIAEoCzI7LnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuSW5ib3VuZE1lc3NhZ2UuRnVuY3Rpb25DYWxsUmVzcG9uc2VIABJQCg92ZXJzaW9uX3JlcXVlc3QYByABKAsyNS5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLkluYm91bmRNZXNzYWdlLlZlcnNpb25SZXF1ZXN0SAAaHAoOVmVyc2lvblJlcXVlc3QSCgoCaWQYASABKA0augcKDkNvbXBpbGVSZXF1ZXN0ElMKBnN0cmluZxgCIAEoCzJBLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuSW5ib3VuZE1lc3NhZ2UuQ29tcGlsZVJlcXVlc3QuU3RyaW5nSW5wdXRIABIOCgRwYXRoGAMgASgJSAASMgoFc3R5bGUYBCABKA4yIy5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLk91dHB1dFN0eWxlEhIKCnNvdXJjZV9tYXAYBSABKAgSUQoJaW1wb3J0ZXJzGAYgAygLMj4uc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5JbmJvdW5kTWVzc2FnZS5Db21waWxlUmVxdWVzdC5JbXBvcnRlchIYChBnbG9iYWxfZnVuY3Rpb25zGAcgAygJEhMKC2FsZXJ0X2NvbG9yGAggASgIEhMKC2FsZXJ0X2FzY2lpGAkgASgIEg8KB3ZlcmJvc2UYCiABKAgSEgoKcXVpZXRfZGVwcxgLIAEoCBIiChpzb3VyY2VfbWFwX2luY2x1ZGVfc291cmNlcxgMIAEoCBIPCgdjaGFyc2V0GA0gASgIEg4KBnNpbGVudBgOIAEoCBIZChFmYXRhbF9kZXByZWNhdGlvbhgPIAMoCRIbChNzaWxlbmNlX2RlcHJlY2F0aW9uGBAgAygJEhoKEmZ1dHVyZV9kZXByZWNhdGlvbhgRIAMoCRIgChhmYXRhbF9kZXByZWNhdGlvbl9leGNlcHQYEiADKAkarAEKC1N0cmluZ0lucHV0Eg4KBnNvdXJjZRgBIAEoCRILCgN1cmwYAiABKAkSLgoGc3ludGF4GAMgASgOMh4uc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5TeW50YXgSUAoIaW1wb3J0ZXIYBCABKAsyPi5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLkluYm91bmRNZXNzYWdlLkNvbXBpbGVSZXF1ZXN0LkltcG9ydGVyGsUBCghJbXBvcnRlchIOCgRwYXRoGAEgASgJSAASFQoLaW1wb3J0ZXJfaWQYAiABKA1IABIaChBmaWxlX2ltcG9ydGVyX2lkGAMgASgNSAASTAoVbm9kZV9wYWNrYWdlX2ltcG9ydGVyGAUgASgLMisuc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5Ob2RlUGFja2FnZUltcG9ydGVySAASHAoUbm9uX2Nhbm9uaWNhbF9zY2hlbWUYBCADKAlCCgoIaW1wb3J0ZXJCBwoFaW5wdXRKBAgBEAIaawoUQ2Fub25pY2FsaXplUmVzcG9uc2USCgoCaWQYASABKA0SDQoDdXJsGAIgASgJSAASDwoFZXJyb3IYAyABKAlIABIdChVjb250YWluaW5nX3VybF91bnVzZWQYBCABKAhCCAoGcmVzdWx0GpMCCg5JbXBvcnRSZXNwb25zZRIKCgJpZBgBIAEoDRJWCgdzdWNjZXNzGAIgASgLMkMuc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5JbmJvdW5kTWVzc2FnZS5JbXBvcnRSZXNwb25zZS5JbXBvcnRTdWNjZXNzSAASDwoFZXJyb3IYAyABKAlIABqBAQoNSW1wb3J0U3VjY2VzcxIQCghjb250ZW50cxgBIAEoCRIuCgZzeW50YXgYAiABKA4yHi5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLlN5bnRheBIbCg5zb3VyY2VfbWFwX3VybBgDIAEoCUgAiAEBQhEKD19zb3VyY2VfbWFwX3VybEIICgZyZXN1bHQabgoSRmlsZUltcG9ydFJlc3BvbnNlEgoKAmlkGAEgASgNEhIKCGZpbGVfdXJsGAIgASgJSAASDwoFZXJyb3IYAyABKAlIABIdChVjb250YWluaW5nX3VybF91bnVzZWQYBCABKAhCCAoGcmVzdWx0GpABChRGdW5jdGlvbkNhbGxSZXNwb25zZRIKCgJpZBgBIAEoDRIwCgdzdWNjZXNzGAIgASgLMh0uc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5WYWx1ZUgAEg8KBWVycm9yGAMgASgJSAASHwoXYWNjZXNzZWRfYXJndW1lbnRfbGlzdHMYBCADKA1CCAoGcmVzdWx0QgkKB21lc3NhZ2Uiyw8KD091dGJvdW5kTWVzc2FnZRI2CgVlcnJvchgBIAEoCzIlLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuUHJvdG9jb2xFcnJvckgAElMKEGNvbXBpbGVfcmVzcG9uc2UYAiABKAsyNy5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLk91dGJvdW5kTWVzc2FnZS5Db21waWxlUmVzcG9uc2VIABJFCglsb2dfZXZlbnQYAyABKAsyMC5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLk91dGJvdW5kTWVzc2FnZS5Mb2dFdmVudEgAElsKFGNhbm9uaWNhbGl6ZV9yZXF1ZXN0GAQgASgLMjsuc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5PdXRib3VuZE1lc3NhZ2UuQ2Fub25pY2FsaXplUmVxdWVzdEgAEk8KDmltcG9ydF9yZXF1ZXN0GAUgASgLMjUuc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5PdXRib3VuZE1lc3NhZ2UuSW1wb3J0UmVxdWVzdEgAElgKE2ZpbGVfaW1wb3J0X3JlcXVlc3QYBiABKAsyOS5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLk91dGJvdW5kTWVzc2FnZS5GaWxlSW1wb3J0UmVxdWVzdEgAElwKFWZ1bmN0aW9uX2NhbGxfcmVxdWVzdBgHIAEoCzI7LnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuT3V0Ym91bmRNZXNzYWdlLkZ1bmN0aW9uQ2FsbFJlcXVlc3RIABJTChB2ZXJzaW9uX3Jlc3BvbnNlGAggASgLMjcuc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5PdXRib3VuZE1lc3NhZ2UuVmVyc2lvblJlc3BvbnNlSAAajgEKD1ZlcnNpb25SZXNwb25zZRIKCgJpZBgFIAEoDRIYChBwcm90b2NvbF92ZXJzaW9uGAEgASgJEhgKEGNvbXBpbGVyX3ZlcnNpb24YAiABKAkSHgoWaW1wbGVtZW50YXRpb25fdmVyc2lvbhgDIAEoCRIbChNpbXBsZW1lbnRhdGlvbl9uYW1lGAQgASgJGqIDCg9Db21waWxlUmVzcG9uc2USWQoHc3VjY2VzcxgCIAEoCzJGLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuT3V0Ym91bmRNZXNzYWdlLkNvbXBpbGVSZXNwb25zZS5Db21waWxlU3VjY2Vzc0gAElkKB2ZhaWx1cmUYAyABKAsyRi5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLk91dGJvdW5kTWVzc2FnZS5Db21waWxlUmVzcG9uc2UuQ29tcGlsZUZhaWx1cmVIABITCgtsb2FkZWRfdXJscxgEIAMoCRo3Cg5Db21waWxlU3VjY2VzcxILCgNjc3MYASABKAkSEgoKc291cmNlX21hcBgCIAEoCUoECAMQBBp7Cg5Db21waWxlRmFpbHVyZRIPCgdtZXNzYWdlGAEgASgJEjAKBHNwYW4YAiABKAsyIi5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLlNvdXJjZVNwYW4SEwoLc3RhY2tfdHJhY2UYAyABKAkSEQoJZm9ybWF0dGVkGAQgASgJQggKBnJlc3VsdEoECAEQAhrxAQoITG9nRXZlbnQSMgoEdHlwZRgCIAEoDjIkLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuTG9nRXZlbnRUeXBlEg8KB21lc3NhZ2UYAyABKAkSNQoEc3BhbhgEIAEoCzIiLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuU291cmNlU3BhbkgAiAEBEhMKC3N0YWNrX3RyYWNlGAUgASgJEhEKCWZvcm1hdHRlZBgGIAEoCRIdChBkZXByZWNhdGlvbl90eXBlGAcgASgJSAGIAQFCBwoFX3NwYW5CEwoRX2RlcHJlY2F0aW9uX3R5cGVKBAgBEAIajgEKE0Nhbm9uaWNhbGl6ZVJlcXVlc3QSCgoCaWQYASABKA0SEwoLaW1wb3J0ZXJfaWQYAyABKA0SCwoDdXJsGAQgASgJEhMKC2Zyb21faW1wb3J0GAUgASgIEhsKDmNvbnRhaW5pbmdfdXJsGAYgASgJSACIAQFCEQoPX2NvbnRhaW5pbmdfdXJsSgQIAhADGkMKDUltcG9ydFJlcXVlc3QSCgoCaWQYASABKA0SEwoLaW1wb3J0ZXJfaWQYAyABKA0SCwoDdXJsGAQgASgJSgQIAhADGowBChFGaWxlSW1wb3J0UmVxdWVzdBIKCgJpZBgBIAEoDRITCgtpbXBvcnRlcl9pZBgDIAEoDRILCgN1cmwYBCABKAkSEwoLZnJvbV9pbXBvcnQYBSABKAgSGwoOY29udGFpbmluZ191cmwYBiABKAlIAIgBAUIRCg9fY29udGFpbmluZ191cmxKBAgCEAMajgEKE0Z1bmN0aW9uQ2FsbFJlcXVlc3QSCgoCaWQYASABKA0SDgoEbmFtZRgDIAEoCUgAEhUKC2Z1bmN0aW9uX2lkGAQgASgNSAASMAoJYXJndW1lbnRzGAUgAygLMh0uc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5WYWx1ZUIMCgppZGVudGlmaWVySgQIAhADQgkKB21lc3NhZ2UiZQoNUHJvdG9jb2xFcnJvchI3CgR0eXBlGAEgASgOMikuc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5Qcm90b2NvbEVycm9yVHlwZRIKCgJpZBgCIAEoDRIPCgdtZXNzYWdlGAMgASgJIocCCgpTb3VyY2VTcGFuEgwKBHRleHQYASABKAkSQAoFc3RhcnQYAiABKAsyMS5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLlNvdXJjZVNwYW4uU291cmNlTG9jYXRpb24SQwoDZW5kGAMgASgLMjEuc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5Tb3VyY2VTcGFuLlNvdXJjZUxvY2F0aW9uSACIAQESCwoDdXJsGAQgASgJEg8KB2NvbnRleHQYBSABKAkaPgoOU291cmNlTG9jYXRpb24SDgoGb2Zmc2V0GAEgASgNEgwKBGxpbmUYAiABKA0SDgoGY29sdW1uGAMgASgNQgYKBF9lbmQi3xIKBVZhbHVlEjYKBnN0cmluZxgBIAEoCzIkLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuVmFsdWUuU3RyaW5nSAASNgoGbnVtYmVyGAIgASgLMiQuc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5WYWx1ZS5OdW1iZXJIABIyCgRsaXN0GAUgASgLMiIuc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5WYWx1ZS5MaXN0SAASMAoDbWFwGAYgASgLMiEuc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5WYWx1ZS5NYXBIABI7CglzaW5nbGV0b24YByABKA4yJi5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLlNpbmdsZXRvblZhbHVlSAASSwoRY29tcGlsZXJfZnVuY3Rpb24YCCABKAsyLi5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLlZhbHVlLkNvbXBpbGVyRnVuY3Rpb25IABJDCg1ob3N0X2Z1bmN0aW9uGAkgASgLMiouc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5WYWx1ZS5Ib3N0RnVuY3Rpb25IABJDCg1hcmd1bWVudF9saXN0GAogASgLMiouc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5WYWx1ZS5Bcmd1bWVudExpc3RIABJACgtjYWxjdWxhdGlvbhgMIAEoCzIpLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuVmFsdWUuQ2FsY3VsYXRpb25IABJFCg5jb21waWxlcl9taXhpbhgNIAEoCzIrLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuVmFsdWUuQ29tcGlsZXJNaXhpbkgAEjQKBWNvbG9yGA4gASgLMiMuc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5WYWx1ZS5Db2xvckgAEkcKD2NvbXBpbGVyX21vZHVsZRgPIAEoCzIsLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuVmFsdWUuQ29tcGlsZXJNb2R1bGVIABomCgZTdHJpbmcSDAoEdGV4dBgBIAEoCRIOCgZxdW90ZWQYAiABKAgaQQoGTnVtYmVyEg0KBXZhbHVlGAEgASgBEhIKCm51bWVyYXRvcnMYAiADKAkSFAoMZGVub21pbmF0b3JzGAMgAygJGqABCgVDb2xvchINCgVzcGFjZRgBIAEoCRIVCghjaGFubmVsMRgCIAEoAUgAiAEBEhUKCGNoYW5uZWwyGAMgASgBSAGIAQESFQoIY2hhbm5lbDMYBCABKAFIAogBARISCgVhbHBoYRgFIAEoAUgDiAEBQgsKCV9jaGFubmVsMUILCglfY2hhbm5lbDJCCwoJX2NoYW5uZWwzQggKBl9hbHBoYRqHAQoETGlzdBI4CglzZXBhcmF0b3IYASABKA4yJS5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLkxpc3RTZXBhcmF0b3ISFAoMaGFzX2JyYWNrZXRzGAIgASgIEi8KCGNvbnRlbnRzGAMgAygLMh0uc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5WYWx1ZRqiAQoDTWFwEjgKB2VudHJpZXMYASADKAsyJy5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLlZhbHVlLk1hcC5FbnRyeRphCgVFbnRyeRIqCgNrZXkYASABKAsyHS5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLlZhbHVlEiwKBXZhbHVlGAIgASgLMh0uc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5WYWx1ZRoeChBDb21waWxlckZ1bmN0aW9uEgoKAmlkGAEgASgNGi0KDEhvc3RGdW5jdGlvbhIKCgJpZBgBIAEoDRIRCglzaWduYXR1cmUYAiABKAkaGwoNQ29tcGlsZXJNaXhpbhIKCgJpZBgBIAEoDRocCg5Db21waWxlck1vZHVsZRIKCgJpZBgBIAEoDRqhAgoMQXJndW1lbnRMaXN0EgoKAmlkGAEgASgNEjgKCXNlcGFyYXRvchgCIAEoDjIlLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuTGlzdFNlcGFyYXRvchIvCghjb250ZW50cxgDIAMoCzIdLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuVmFsdWUSSgoIa2V5d29yZHMYBCADKAsyOC5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLlZhbHVlLkFyZ3VtZW50TGlzdC5LZXl3b3Jkc0VudHJ5Gk4KDUtleXdvcmRzRW50cnkSCwoDa2V5GAEgASgJEiwKBXZhbHVlGAIgASgLMh0uc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5WYWx1ZToCOAEa7wQKC0NhbGN1bGF0aW9uEgwKBG5hbWUYASABKAkSTQoJYXJndW1lbnRzGAIgAygLMjouc2Fzcy5lbWJlZGRlZF9wcm90b2NvbC5WYWx1ZS5DYWxjdWxhdGlvbi5DYWxjdWxhdGlvblZhbHVlGpUCChBDYWxjdWxhdGlvblZhbHVlEjYKBm51bWJlchgBIAEoCzIkLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuVmFsdWUuTnVtYmVySAASEAoGc3RyaW5nGAIgASgJSAASFwoNaW50ZXJwb2xhdGlvbhgDIAEoCUgAElMKCW9wZXJhdGlvbhgEIAEoCzI+LnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuVmFsdWUuQ2FsY3VsYXRpb24uQ2FsY3VsYXRpb25PcGVyYXRpb25IABJACgtjYWxjdWxhdGlvbhgFIAEoCzIpLnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuVmFsdWUuQ2FsY3VsYXRpb25IAEIHCgV2YWx1ZRrqAQoUQ2FsY3VsYXRpb25PcGVyYXRpb24SPQoIb3BlcmF0b3IYASABKA4yKy5zYXNzLmVtYmVkZGVkX3Byb3RvY29sLkNhbGN1bGF0aW9uT3BlcmF0b3ISSAoEbGVmdBgCIAEoCzI6LnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuVmFsdWUuQ2FsY3VsYXRpb24uQ2FsY3VsYXRpb25WYWx1ZRJJCgVyaWdodBgDIAEoCzI6LnNhc3MuZW1iZWRkZWRfcHJvdG9jb2wuVmFsdWUuQ2FsY3VsYXRpb24uQ2FsY3VsYXRpb25WYWx1ZUIHCgV2YWx1ZSI0ChNOb2RlUGFja2FnZUltcG9ydGVyEh0KFWVudHJ5X3BvaW50X2RpcmVjdG9yeRgBIAEoCSorCgtPdXRwdXRTdHlsZRIMCghFWFBBTkRFRBAAEg4KCkNPTVBSRVNTRUQQASopCgZTeW50YXgSCAoEU0NTUxAAEgwKCElOREVOVEVEEAESBwoDQ1NTEAIqPwoMTG9nRXZlbnRUeXBlEgsKB1dBUk5JTkcQABIXChNERVBSRUNBVElPTl9XQVJOSU5HEAESCQoFREVCVUcQAio4ChFQcm90b2NvbEVycm9yVHlwZRIJCgVQQVJTRRAAEgoKBlBBUkFNUxABEgwKCElOVEVSTkFMEAIqPwoNTGlzdFNlcGFyYXRvchIJCgVDT01NQRAAEgkKBVNQQUNFEAESCQoFU0xBU0gQAhINCglVTkRFQ0lERUQQAyovCg5TaW5nbGV0b25WYWx1ZRIICgRUUlVFEAASCQoFRkFMU0UQARIICgROVUxMEAIqQQoTQ2FsY3VsYXRpb25PcGVyYXRvchIICgRQTFVTEAASCQoFTUlOVVMQARIJCgVUSU1FUxACEgoKBkRJVklERRADQiMKH2NvbS5zYXNzX2xhbmcuZW1iZWRkZWRfcHJvdG9jb2xQAWIGcHJvdG8z");
/**
 * Describes the message sass.embedded_protocol.InboundMessage.
 * Use `create(InboundMessageSchema)` to create a new message.
 */
exports.InboundMessageSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 0);
/**
 * Describes the message sass.embedded_protocol.InboundMessage.VersionRequest.
 * Use `create(InboundMessage_VersionRequestSchema)` to create a new message.
 */
exports.InboundMessage_VersionRequestSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 0, 0);
/**
 * Describes the message sass.embedded_protocol.InboundMessage.CompileRequest.
 * Use `create(InboundMessage_CompileRequestSchema)` to create a new message.
 */
exports.InboundMessage_CompileRequestSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 0, 1);
/**
 * Describes the message sass.embedded_protocol.InboundMessage.CompileRequest.StringInput.
 * Use `create(InboundMessage_CompileRequest_StringInputSchema)` to create a new message.
 */
exports.InboundMessage_CompileRequest_StringInputSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 0, 1, 0);
/**
 * Describes the message sass.embedded_protocol.InboundMessage.CompileRequest.Importer.
 * Use `create(InboundMessage_CompileRequest_ImporterSchema)` to create a new message.
 */
exports.InboundMessage_CompileRequest_ImporterSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 0, 1, 1);
/**
 * Describes the message sass.embedded_protocol.InboundMessage.CanonicalizeResponse.
 * Use `create(InboundMessage_CanonicalizeResponseSchema)` to create a new message.
 */
exports.InboundMessage_CanonicalizeResponseSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 0, 2);
/**
 * Describes the message sass.embedded_protocol.InboundMessage.ImportResponse.
 * Use `create(InboundMessage_ImportResponseSchema)` to create a new message.
 */
exports.InboundMessage_ImportResponseSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 0, 3);
/**
 * Describes the message sass.embedded_protocol.InboundMessage.ImportResponse.ImportSuccess.
 * Use `create(InboundMessage_ImportResponse_ImportSuccessSchema)` to create a new message.
 */
exports.InboundMessage_ImportResponse_ImportSuccessSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 0, 3, 0);
/**
 * Describes the message sass.embedded_protocol.InboundMessage.FileImportResponse.
 * Use `create(InboundMessage_FileImportResponseSchema)` to create a new message.
 */
exports.InboundMessage_FileImportResponseSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 0, 4);
/**
 * Describes the message sass.embedded_protocol.InboundMessage.FunctionCallResponse.
 * Use `create(InboundMessage_FunctionCallResponseSchema)` to create a new message.
 */
exports.InboundMessage_FunctionCallResponseSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 0, 5);
/**
 * Describes the message sass.embedded_protocol.OutboundMessage.
 * Use `create(OutboundMessageSchema)` to create a new message.
 */
exports.OutboundMessageSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 1);
/**
 * Describes the message sass.embedded_protocol.OutboundMessage.VersionResponse.
 * Use `create(OutboundMessage_VersionResponseSchema)` to create a new message.
 */
exports.OutboundMessage_VersionResponseSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 1, 0);
/**
 * Describes the message sass.embedded_protocol.OutboundMessage.CompileResponse.
 * Use `create(OutboundMessage_CompileResponseSchema)` to create a new message.
 */
exports.OutboundMessage_CompileResponseSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 1, 1);
/**
 * Describes the message sass.embedded_protocol.OutboundMessage.CompileResponse.CompileSuccess.
 * Use `create(OutboundMessage_CompileResponse_CompileSuccessSchema)` to create a new message.
 */
exports.OutboundMessage_CompileResponse_CompileSuccessSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 1, 1, 0);
/**
 * Describes the message sass.embedded_protocol.OutboundMessage.CompileResponse.CompileFailure.
 * Use `create(OutboundMessage_CompileResponse_CompileFailureSchema)` to create a new message.
 */
exports.OutboundMessage_CompileResponse_CompileFailureSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 1, 1, 1);
/**
 * Describes the message sass.embedded_protocol.OutboundMessage.LogEvent.
 * Use `create(OutboundMessage_LogEventSchema)` to create a new message.
 */
exports.OutboundMessage_LogEventSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 1, 2);
/**
 * Describes the message sass.embedded_protocol.OutboundMessage.CanonicalizeRequest.
 * Use `create(OutboundMessage_CanonicalizeRequestSchema)` to create a new message.
 */
exports.OutboundMessage_CanonicalizeRequestSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 1, 3);
/**
 * Describes the message sass.embedded_protocol.OutboundMessage.ImportRequest.
 * Use `create(OutboundMessage_ImportRequestSchema)` to create a new message.
 */
exports.OutboundMessage_ImportRequestSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 1, 4);
/**
 * Describes the message sass.embedded_protocol.OutboundMessage.FileImportRequest.
 * Use `create(OutboundMessage_FileImportRequestSchema)` to create a new message.
 */
exports.OutboundMessage_FileImportRequestSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 1, 5);
/**
 * Describes the message sass.embedded_protocol.OutboundMessage.FunctionCallRequest.
 * Use `create(OutboundMessage_FunctionCallRequestSchema)` to create a new message.
 */
exports.OutboundMessage_FunctionCallRequestSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 1, 6);
/**
 * Describes the message sass.embedded_protocol.ProtocolError.
 * Use `create(ProtocolErrorSchema)` to create a new message.
 */
exports.ProtocolErrorSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 2);
/**
 * Describes the message sass.embedded_protocol.SourceSpan.
 * Use `create(SourceSpanSchema)` to create a new message.
 */
exports.SourceSpanSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 3);
/**
 * Describes the message sass.embedded_protocol.SourceSpan.SourceLocation.
 * Use `create(SourceSpan_SourceLocationSchema)` to create a new message.
 */
exports.SourceSpan_SourceLocationSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 3, 0);
/**
 * Describes the message sass.embedded_protocol.Value.
 * Use `create(ValueSchema)` to create a new message.
 */
exports.ValueSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4);
/**
 * Describes the message sass.embedded_protocol.Value.String.
 * Use `create(Value_StringSchema)` to create a new message.
 */
exports.Value_StringSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 0);
/**
 * Describes the message sass.embedded_protocol.Value.Number.
 * Use `create(Value_NumberSchema)` to create a new message.
 */
exports.Value_NumberSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 1);
/**
 * Describes the message sass.embedded_protocol.Value.Color.
 * Use `create(Value_ColorSchema)` to create a new message.
 */
exports.Value_ColorSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 2);
/**
 * Describes the message sass.embedded_protocol.Value.List.
 * Use `create(Value_ListSchema)` to create a new message.
 */
exports.Value_ListSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 3);
/**
 * Describes the message sass.embedded_protocol.Value.Map.
 * Use `create(Value_MapSchema)` to create a new message.
 */
exports.Value_MapSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 4);
/**
 * Describes the message sass.embedded_protocol.Value.Map.Entry.
 * Use `create(Value_Map_EntrySchema)` to create a new message.
 */
exports.Value_Map_EntrySchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 4, 0);
/**
 * Describes the message sass.embedded_protocol.Value.CompilerFunction.
 * Use `create(Value_CompilerFunctionSchema)` to create a new message.
 */
exports.Value_CompilerFunctionSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 5);
/**
 * Describes the message sass.embedded_protocol.Value.HostFunction.
 * Use `create(Value_HostFunctionSchema)` to create a new message.
 */
exports.Value_HostFunctionSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 6);
/**
 * Describes the message sass.embedded_protocol.Value.CompilerMixin.
 * Use `create(Value_CompilerMixinSchema)` to create a new message.
 */
exports.Value_CompilerMixinSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 7);
/**
 * Describes the message sass.embedded_protocol.Value.CompilerModule.
 * Use `create(Value_CompilerModuleSchema)` to create a new message.
 */
exports.Value_CompilerModuleSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 8);
/**
 * Describes the message sass.embedded_protocol.Value.ArgumentList.
 * Use `create(Value_ArgumentListSchema)` to create a new message.
 */
exports.Value_ArgumentListSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 9);
/**
 * Describes the message sass.embedded_protocol.Value.Calculation.
 * Use `create(Value_CalculationSchema)` to create a new message.
 */
exports.Value_CalculationSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 10);
/**
 * Describes the message sass.embedded_protocol.Value.Calculation.CalculationValue.
 * Use `create(Value_Calculation_CalculationValueSchema)` to create a new message.
 */
exports.Value_Calculation_CalculationValueSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 10, 0);
/**
 * Describes the message sass.embedded_protocol.Value.Calculation.CalculationOperation.
 * Use `create(Value_Calculation_CalculationOperationSchema)` to create a new message.
 */
exports.Value_Calculation_CalculationOperationSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 4, 10, 1);
/**
 * Describes the message sass.embedded_protocol.NodePackageImporter.
 * Use `create(NodePackageImporterSchema)` to create a new message.
 */
exports.NodePackageImporterSchema = (0, codegenv2_1.messageDesc)(exports.file_embedded_sass, 5);
/**
 * Possible ways to format the CSS output. The compiler is not required to
 * support all possible options; if the host requests an unsupported style, the
 * compiler should choose the closest supported style.
 *
 * @generated from enum sass.embedded_protocol.OutputStyle
 */
var OutputStyle;
(function (OutputStyle) {
    /**
     * Each selector and declaration is written on its own line.
     *
     * @generated from enum value: EXPANDED = 0;
     */
    OutputStyle[OutputStyle["EXPANDED"] = 0] = "EXPANDED";
    /**
     * The entire stylesheet is written on a single line, with as few characters
     * as possible.
     *
     * @generated from enum value: COMPRESSED = 1;
     */
    OutputStyle[OutputStyle["COMPRESSED"] = 1] = "COMPRESSED";
})(OutputStyle || (exports.OutputStyle = OutputStyle = {}));
/**
 * Describes the enum sass.embedded_protocol.OutputStyle.
 */
exports.OutputStyleSchema = (0, codegenv2_1.enumDesc)(exports.file_embedded_sass, 0);
/**
 * Possible syntaxes for a Sass stylesheet.
 *
 * @generated from enum sass.embedded_protocol.Syntax
 */
var Syntax;
(function (Syntax) {
    /**
     * The CSS-superset `.scss` syntax.
     *
     * @generated from enum value: SCSS = 0;
     */
    Syntax[Syntax["SCSS"] = 0] = "SCSS";
    /**
     * The indented `.sass` syntax.
     *
     * @generated from enum value: INDENTED = 1;
     */
    Syntax[Syntax["INDENTED"] = 1] = "INDENTED";
    /**
     * Plain CSS syntax that doesn't support any special Sass features.
     *
     * @generated from enum value: CSS = 2;
     */
    Syntax[Syntax["CSS"] = 2] = "CSS";
})(Syntax || (exports.Syntax = Syntax = {}));
/**
 * Describes the enum sass.embedded_protocol.Syntax.
 */
exports.SyntaxSchema = (0, codegenv2_1.enumDesc)(exports.file_embedded_sass, 1);
/**
 * The possible types of [LogEvent].
 *
 * @generated from enum sass.embedded_protocol.LogEventType
 */
var LogEventType;
(function (LogEventType) {
    /**
     * A warning for something other than a deprecated Sass feature. Often emitted
     * due to a stylesheet using the `@warn` rule.
     *
     * @generated from enum value: WARNING = 0;
     */
    LogEventType[LogEventType["WARNING"] = 0] = "WARNING";
    /**
     * A warning indicating that the stylesheet is using a deprecated Sass
     * feature. Compilers should not add text like "deprecation warning" to
     * deprecation warnings; it's up to the host to determine how to signal that
     * to the user.
     *
     * @generated from enum value: DEPRECATION_WARNING = 1;
     */
    LogEventType[LogEventType["DEPRECATION_WARNING"] = 1] = "DEPRECATION_WARNING";
    /**
     * A message generated by the user for their own debugging purposes.
     *
     * @generated from enum value: DEBUG = 2;
     */
    LogEventType[LogEventType["DEBUG"] = 2] = "DEBUG";
})(LogEventType || (exports.LogEventType = LogEventType = {}));
/**
 * Describes the enum sass.embedded_protocol.LogEventType.
 */
exports.LogEventTypeSchema = (0, codegenv2_1.enumDesc)(exports.file_embedded_sass, 2);
/**
 * Potential types of protocol errors.
 *
 * @generated from enum sass.embedded_protocol.ProtocolErrorType
 */
var ProtocolErrorType;
(function (ProtocolErrorType) {
    /**
     * A message was received that couldn't be decoded as an `InboundMessage` (for
     * the compiler) or `OutboundMessage` (for the host).
     *
     * @generated from enum value: PARSE = 0;
     */
    ProtocolErrorType[ProtocolErrorType["PARSE"] = 0] = "PARSE";
    /**
     * A message was received that violated a documented restriction, such as not
     * providing a mandatory field.
     *
     * @generated from enum value: PARAMS = 1;
     */
    ProtocolErrorType[ProtocolErrorType["PARAMS"] = 1] = "PARAMS";
    /**
     * Something unexpected went wrong within the endpoint.
     *
     * @generated from enum value: INTERNAL = 2;
     */
    ProtocolErrorType[ProtocolErrorType["INTERNAL"] = 2] = "INTERNAL";
})(ProtocolErrorType || (exports.ProtocolErrorType = ProtocolErrorType = {}));
/**
 * Describes the enum sass.embedded_protocol.ProtocolErrorType.
 */
exports.ProtocolErrorTypeSchema = (0, codegenv2_1.enumDesc)(exports.file_embedded_sass, 3);
/**
 * Different types of separators a list can have.
 *
 * @generated from enum sass.embedded_protocol.ListSeparator
 */
var ListSeparator;
(function (ListSeparator) {
    /**
     * List elements are separated by a comma.
     *
     * @generated from enum value: COMMA = 0;
     */
    ListSeparator[ListSeparator["COMMA"] = 0] = "COMMA";
    /**
     * List elements are separated by whitespace.
     *
     * @generated from enum value: SPACE = 1;
     */
    ListSeparator[ListSeparator["SPACE"] = 1] = "SPACE";
    /**
     * List elements are separated by a forward slash.
     *
     * @generated from enum value: SLASH = 2;
     */
    ListSeparator[ListSeparator["SLASH"] = 2] = "SLASH";
    /**
     * The list's separator hasn't yet been determined. This is only allowed for
     * singleton and empty lists.
     *
     * Singleton lists and empty lists don't have separators defined. This means
     * that list functions will prefer other lists' separators if possible.
     *
     * @generated from enum value: UNDECIDED = 3;
     */
    ListSeparator[ListSeparator["UNDECIDED"] = 3] = "UNDECIDED";
})(ListSeparator || (exports.ListSeparator = ListSeparator = {}));
/**
 * Describes the enum sass.embedded_protocol.ListSeparator.
 */
exports.ListSeparatorSchema = (0, codegenv2_1.enumDesc)(exports.file_embedded_sass, 4);
/**
 * Singleton SassScript values that have no internal state.
 *
 * @generated from enum sass.embedded_protocol.SingletonValue
 */
var SingletonValue;
(function (SingletonValue) {
    /**
     * The SassScript boolean true value.
     *
     * @generated from enum value: TRUE = 0;
     */
    SingletonValue[SingletonValue["TRUE"] = 0] = "TRUE";
    /**
     * The SassScript boolean false value.
     *
     * @generated from enum value: FALSE = 1;
     */
    SingletonValue[SingletonValue["FALSE"] = 1] = "FALSE";
    /**
     * The SassScript null value.
     *
     * @generated from enum value: NULL = 2;
     */
    SingletonValue[SingletonValue["NULL"] = 2] = "NULL";
})(SingletonValue || (exports.SingletonValue = SingletonValue = {}));
/**
 * Describes the enum sass.embedded_protocol.SingletonValue.
 */
exports.SingletonValueSchema = (0, codegenv2_1.enumDesc)(exports.file_embedded_sass, 5);
/**
 * An operator used in a calculation value's operation.
 *
 * @generated from enum sass.embedded_protocol.CalculationOperator
 */
var CalculationOperator;
(function (CalculationOperator) {
    /**
     * The addition operator.
     *
     * @generated from enum value: PLUS = 0;
     */
    CalculationOperator[CalculationOperator["PLUS"] = 0] = "PLUS";
    /**
     * The subtraction operator.
     *
     * @generated from enum value: MINUS = 1;
     */
    CalculationOperator[CalculationOperator["MINUS"] = 1] = "MINUS";
    /**
     * The multiplication operator.
     *
     * @generated from enum value: TIMES = 2;
     */
    CalculationOperator[CalculationOperator["TIMES"] = 2] = "TIMES";
    /**
     * The division operator.
     *
     * @generated from enum value: DIVIDE = 3;
     */
    CalculationOperator[CalculationOperator["DIVIDE"] = 3] = "DIVIDE";
})(CalculationOperator || (exports.CalculationOperator = CalculationOperator = {}));
/**
 * Describes the enum sass.embedded_protocol.CalculationOperator.
 */
exports.CalculationOperatorSchema = (0, codegenv2_1.enumDesc)(exports.file_embedded_sass, 6);
//# sourceMappingURL=embedded_sass_pb.js.map